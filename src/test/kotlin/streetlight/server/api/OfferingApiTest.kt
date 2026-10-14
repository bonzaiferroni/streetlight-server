package streetlight.server.api

import streetlight.model.Api
import streetlight.model.data.OfferingEdit
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals

class OfferingApiTest : ApiTest() {

    @Test
    fun `a star creates an offering and finds it among their own`() = runApiTest {
        registerUser("alice")
        signInAs("alice")

        val offering = postApi(Api.Offerings.Create, offeringEdit()).toDataOrThrow()

        val offerings = getApi(Api.Offerings.ReadMine).toDataOrThrow()
        assertEquals(listOf(offering), offerings, "the star should find the offering they created")
        assertEquals("usd", offering.currency, "a star with no seller account should price in the default currency")
    }

    @Test
    fun `a star updates their offering`() = runApiTest {
        registerUser("alice")
        signInAs("alice")
        val offering = postApi(Api.Offerings.Create, offeringEdit()).toDataOrThrow()

        postApi(Api.Offerings.Update, offeringEdit().copy(offeringId = offering.offeringId, title = "Site Rescue"))
            .toDataOrThrow()

        assertEquals("Site Rescue", getApi(Api.Offerings.ReadMine).toDataOrThrow().single().title)
    }

    @Test
    fun `a star cannot update the offering of another star`() = runApiTest {
        registerUser("alice")
        signInAs("alice")
        val offering = postApi(Api.Offerings.Create, offeringEdit()).toDataOrThrow()

        registerUser("bob")
        signInAs("bob")
        postApi(Api.Offerings.Update, offeringEdit().copy(offeringId = offering.offeringId, title = "Taken"))
            .toProblemOrThrow()

        signInAs("alice")
        assertEquals("Website Build", getApi(Api.Offerings.ReadMine).toDataOrThrow().single().title,
            "the offering should keep its title")
    }
}

private fun offeringEdit() = OfferingEdit(title = "Website Build", priceCents = 50_000)
