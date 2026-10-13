package streetlight.server.api

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import klutch.utils.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import streetlight.model.Api
import streetlight.model.data.CapabilityStatus
import streetlight.model.data.Seller
import streetlight.model.data.SellerOnboarding
import streetlight.model.data.StarId
import streetlight.server.TEST_STRIPE_WEBHOOK_SECRET
import streetlight.server.db.tables.SellerTable
import streetlight.server.db.tables.toSeller
import streetlight.server.external.signStripePayload
import streetlight.server.loginStar
import streetlight.server.model.TestPaymentClient
import streetlight.server.registerStar
import streetlight.server.toDataOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class SellerApiTest : ApiTest() {

    private val payments get() = server.client.payment as TestPaymentClient

    @Test
    fun `a star starts selling and their seller account is stored`() = runApiTest {
        val starId = server.registerStar()
        signIn(server.loginStar())

        val url = postApi(Api.Sellers.Onboard, SellerOnboarding("US")).toDataOrThrow()

        val seller = assertNotNull(sellerOrNull(starId), "onboarding should store a seller")
        assertEquals("https://connect.stripe.test/setup/${seller.providerAccountId}", url.value)
        assertEquals(CapabilityStatus.Restricted, seller.chargeStatus, "a new seller should not take charges before onboarding")
        assertTrue(seller.isAwaitingSeller, "a new seller should owe their details")
    }

    @Test
    fun `a seller who onboards again keeps their account`() = runApiTest {
        val starId = server.registerStar()
        signIn(server.loginStar())

        postApi(Api.Sellers.Onboard, SellerOnboarding("US")).toDataOrThrow()
        val first = sellerOrNull(starId)
        postApi(Api.Sellers.Onboard, SellerOnboarding("US")).toDataOrThrow()

        assertEquals(1, payments.accounts.size, "a second onboarding should not create an account")
        assertEquals(first?.providerAccountId, sellerOrNull(starId)?.providerAccountId)
    }

    @Test
    fun `a seller returning from onboarding reads the status the provider reports`() = runApiTest {
        val starId = server.registerStar()
        signIn(server.loginStar())
        postApi(Api.Sellers.Onboard, SellerOnboarding("US")).toDataOrThrow()
        completeOnboarding(starId)

        val seller = getApi(Api.Sellers.ReadStatus).toDataOrThrow()

        assertEquals(CapabilityStatus.Active, seller?.chargeStatus, "the status should show charges active")
        assertEquals(CapabilityStatus.Active, sellerOrNull(starId)?.chargeStatus, "the stored seller should take charges")
    }

    @Test
    fun `a star who has not started selling reads no seller`() = runApiTest {
        server.registerStar()
        signIn(server.loginStar())

        assertNull(getApi(Api.Sellers.ReadStatus).toDataOrThrow())
    }

    @Test
    fun `an account update event enables charges for the seller`() = runApiTest {
        val starId = server.registerStar()
        signIn(server.loginStar())
        postApi(Api.Sellers.Onboard, SellerOnboarding("US")).toDataOrThrow()
        val accountId = completeOnboarding(starId)

        val status = postWebhook(accountEvent("v2.core.account.updated", accountId))

        assertEquals(HttpStatusCode.OK, status)
        assertEquals(CapabilityStatus.Active, sellerOrNull(starId)?.chargeStatus, "the event should enable charges")
    }

    @Test
    fun `an event with a bad signature changes nothing`() = runApiTest {
        val starId = server.registerStar()
        signIn(server.loginStar())
        postApi(Api.Sellers.Onboard, SellerOnboarding("US")).toDataOrThrow()
        val accountId = completeOnboarding(starId)

        val status = postWebhook(accountEvent("v2.core.account.updated", accountId), secret = "whsec_wrong")

        assertEquals(HttpStatusCode.BadRequest, status)
        assertEquals(CapabilityStatus.Restricted, sellerOrNull(starId)?.chargeStatus, "an unsigned event should not change the seller")
    }

    @Test
    fun `a seller whose account is closed is no longer a seller`() = runApiTest {
        val starId = server.registerStar()
        signIn(server.loginStar())
        postApi(Api.Sellers.Onboard, SellerOnboarding("US")).toDataOrThrow()
        val accountId = sellerOrNull(starId)!!.providerAccountId

        postWebhook(accountEvent("v2.core.account.closed", accountId))

        assertNull(sellerOrNull(starId), "the seller should be removed")
    }

    /** Marks the seller's provider account as fully onboarded, returning its id. */
    private fun completeOnboarding(starId: StarId): String {
        val accountId = sellerOrNull(starId)!!.providerAccountId
        payments.accounts[accountId] = payments.accounts.getValue(accountId).copy(
            chargeStatus = CapabilityStatus.Active,
            payoutStatus = CapabilityStatus.Active,
            isAwaitingSeller = false,
            statusReason = null,
        )
        return accountId
    }

    private suspend fun ApiTestScope.postWebhook(
        payload: String,
        secret: String = TEST_STRIPE_WEBHOOK_SECRET,
    ): HttpStatusCode {
        val timestamp = Clock.System.now().epochSeconds
        val signature = signStripePayload(payload, timestamp, secret)
        return http.post("/webhooks/stripe/connect") {
            header("Stripe-Signature", "t=$timestamp,v1=$signature")
            setBody(payload)
        }.status
    }
}

private fun accountEvent(type: String, accountId: String) = """
    {
        "id": "evt_test",
        "object": "v2.core.event",
        "type": "$type",
        "created": "2026-10-13T12:00:00.000Z",
        "context": null,
        "related_object": { "id": "$accountId", "type": "v2.core.account", "url": "/v2/core/accounts/$accountId" }
    }
""".trimIndent()

private fun sellerOrNull(starId: StarId): Seller? = transaction {
    SellerTable.selectAll().where { SellerTable.starId.eq(starId) }.singleOrNull()?.toSeller()
}
