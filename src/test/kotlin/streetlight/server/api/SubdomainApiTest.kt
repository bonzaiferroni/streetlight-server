package streetlight.server.api

import kampfire.api.Slug
import kampfire.model.CoreProblem
import kampfire.model.HttpProblem
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import streetlight.model.Api
import streetlight.model.data.LocationId
import streetlight.model.data.SubdomainConfig
import streetlight.server.db.tables.SubdomainTable
import streetlight.server.loginStar
import streetlight.server.registerAdmin
import streetlight.server.registerStar
import streetlight.server.seedLocation
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubdomainApiTest : ApiTest() {

    @Test
    fun `a user who is not an admin cannot set a subdomain`() = runApiTest {
        val starId = server.registerStar()
        val location = server.seedLocation(starId)
        signIn(server.loginStar())

        postApi(Api.Locations.UpdateSubdomain, SubdomainConfig(location.locationId, Slug("foxden"))).toProblemOrThrow()

        assertNull(subdomainOf(location.locationId), "a user should not claim a subdomain")
    }

    @Test
    fun `an admin sets a subdomain for a location`() = runApiTest {
        val starId = server.registerAdmin()
        val location = server.seedLocation(starId)
        signIn(server.loginStar())

        postApi(Api.Locations.UpdateSubdomain, SubdomainConfig(location.locationId, Slug("foxden"))).toDataOrThrow()

        assertEquals("foxden", subdomainOf(location.locationId))
    }

    @Test
    fun `a reserved subdomain is refused`() = runApiTest {
        val starId = server.registerAdmin()
        val location = server.seedLocation(starId)
        signIn(server.loginStar())

        val problem = postApi(Api.Locations.UpdateSubdomain, SubdomainConfig(location.locationId, Slug("admin"))).toProblemOrThrow()

        assertEquals(HttpProblem.Conflict, problem)
        assertNull(subdomainOf(location.locationId), "a reserved name should not be stored")
    }

    @Test
    fun `a subdomain already held by another location is refused`() = runApiTest {
        val starId = server.registerAdmin()
        val first = server.seedLocation(starId, "The Fox Den")
        val second = server.seedLocation(starId, "The Owl Nest")
        signIn(server.loginStar())
        postApi(Api.Locations.UpdateSubdomain, SubdomainConfig(first.locationId, Slug("foxden"))).toDataOrThrow()

        val problem = postApi(Api.Locations.UpdateSubdomain, SubdomainConfig(second.locationId, Slug("foxden"))).toProblemOrThrow()

        assertEquals(HttpProblem.Conflict, problem)
        assertEquals("foxden", subdomainOf(first.locationId), "the first location should keep its subdomain")
        assertNull(subdomainOf(second.locationId), "the second location should get none")
    }

    @Test
    fun `an invalid subdomain is refused`() = runApiTest {
        val starId = server.registerAdmin()
        val location = server.seedLocation(starId)
        signIn(server.loginStar())

        val problem = postApi(Api.Locations.UpdateSubdomain, SubdomainConfig(location.locationId, Slug("Fox Den!"))).toProblemOrThrow()

        assertEquals(CoreProblem.InvalidSlug, problem)
        assertNull(subdomainOf(location.locationId), "an invalid name should not be stored")
    }

    @Test
    fun `an admin is told whether a subdomain is available`() = runApiTest {
        val starId = server.registerAdmin()
        val first = server.seedLocation(starId, "The Fox Den")
        val second = server.seedLocation(starId, "The Owl Nest")
        signIn(server.loginStar())
        postApi(Api.Locations.UpdateSubdomain, SubdomainConfig(first.locationId, Slug("foxden"))).toDataOrThrow()

        val free = postApi(Api.Locations.CheckSubdomain, SubdomainConfig(second.locationId, Slug("owlnest"))).toDataOrThrow()
        val held = postApi(Api.Locations.CheckSubdomain, SubdomainConfig(second.locationId, Slug("foxden"))).toDataOrThrow()
        val own = postApi(Api.Locations.CheckSubdomain, SubdomainConfig(first.locationId, Slug("foxden"))).toDataOrThrow()
        val reserved = postApi(Api.Locations.CheckSubdomain, SubdomainConfig(second.locationId, Slug("admin"))).toDataOrThrow()

        assertEquals(true, free, "an unused name should be available")
        assertEquals(false, held, "a name held by another location should not be available")
        assertEquals(true, own, "a location's own name should be available to it")
        assertEquals(false, reserved, "a reserved name should not be available")
    }

    @Test
    fun `an admin removes a subdomain`() = runApiTest {
        val starId = server.registerAdmin()
        val location = server.seedLocation(starId)
        signIn(server.loginStar())
        postApi(Api.Locations.UpdateSubdomain, SubdomainConfig(location.locationId, Slug("foxden"))).toDataOrThrow()

        postApi(Api.Locations.UpdateSubdomain, SubdomainConfig(location.locationId, null)).toDataOrThrow()

        assertNull(subdomainOf(location.locationId), "the subdomain should be removed")
    }

    private fun subdomainOf(locationId: LocationId): String? = transaction {
        SubdomainTable.selectAll()
            .where { SubdomainTable.locationId.eq(locationId) }
            .singleOrNull()?.get(SubdomainTable.slug)
    }
}
