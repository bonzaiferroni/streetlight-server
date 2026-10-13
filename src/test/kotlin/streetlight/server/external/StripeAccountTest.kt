package streetlight.server.external

import streetlight.model.data.CapabilityStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StripeAccountTest {

    @Test
    fun `a new account awaits the seller and is restricted from charges`() {
        val account = readAccount(
            cardPayments = """{ "status": "restricted", "status_details": [{ "code": "requirements_past_due" }] }""",
            requirements = """[{ "awaiting_action_from": "user", "minimum_deadline": { "status": "currently_due" } }]""",
        )

        assertTrue(account.isAwaitingSeller, "details due now from the seller should await them")
        assertEquals(CapabilityStatus.Restricted, account.chargeStatus)
        assertEquals("requirements_past_due", account.statusReason)
    }

    @Test
    fun `an account under review by Stripe is pending and awaits nothing from the seller`() {
        val account = readAccount(
            cardPayments = """{ "status": "pending", "status_details": [] }""",
            requirements = """[{ "awaiting_action_from": "stripe", "minimum_deadline": { "status": "currently_due" } }]""",
        )

        assertFalse(account.isAwaitingSeller, "requirements waiting on Stripe should not await the seller")
        assertEquals(CapabilityStatus.Pending, account.chargeStatus)
    }

    @Test
    fun `a restricted account whose due requirements all await Stripe is under review`() {
        val account = readAccount(
            cardPayments = """{ "status": "restricted", "status_details": [{ "code": "requirements_past_due" }] }""",
            requirements = """[{ "awaiting_action_from": "stripe", "minimum_deadline": { "status": "past_due" } }]""",
        )

        assertEquals(CapabilityStatus.Pending, account.chargeStatus, "verification by Stripe should read as pending")
        assertFalse(account.isAwaitingSeller)
        assertEquals(null, account.statusReason, "a pending account should carry no restriction reason")
    }

    @Test
    fun `an active account takes charges and pays out`() {
        val account = readAccount(
            cardPayments = """{ "status": "active", "status_details": [] }""",
            requirements = """[{ "awaiting_action_from": "user", "minimum_deadline": { "status": "eventually_due" } }]""",
        )

        assertFalse(account.isAwaitingSeller, "a requirement only eventually due should not await the seller")
        assertEquals(CapabilityStatus.Active, account.chargeStatus)
        assertEquals(CapabilityStatus.Active, account.payoutStatus)
        assertEquals("US", account.country)
        assertEquals(null, account.statusReason)
    }

    private fun readAccount(cardPayments: String, requirements: String) = stripeJson.decodeFromString<StripeAccount>("""
        {
            "id": "acct_test",
            "object": "v2.core.account",
            "identity": { "country": "us" },
            "defaults": { "currency": "usd" },
            "configuration": { "merchant": { "capabilities": {
                "card_payments": $cardPayments,
                "stripe_balance": { "payouts": { "status": "active", "status_details": [] } }
            } } },
            "requirements": { "entries": $requirements }
        }
    """).toProviderAccount()
}
