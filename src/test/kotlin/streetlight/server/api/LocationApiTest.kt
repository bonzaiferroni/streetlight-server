package streetlight.server.api

import kampfire.api.EmailAddress
import kampfire.api.Username
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.Api
import streetlight.model.data.EditType
import streetlight.model.data.LocationConfig
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationId
import streetlight.model.data.ParseMode
import streetlight.model.data.StarId
import streetlight.server.LOCATION_NAME
import streetlight.server.TestDefault
import streetlight.server.db.tables.EditLogTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.locationEdit
import streetlight.server.loginStar
import streetlight.server.registerAdmin
import streetlight.server.seedLocation
import streetlight.server.registerStar
import streetlight.server.seedCity
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals

class LocationApiTest : ApiTest() {

    @Test
    fun `a signed-out user cannot create a location`() = runApiTest {
        server.seedCity()

        postApi(Api.Locations.CreateLocation, locationEdit()).toProblemOrThrow()

        assertEquals(0, locationCount(), "a signed-out user should leave no location behind")
    }

    @Test
    fun `a user creates a location and is recorded as its scout`() = runApiTest {
        server.seedCity()
        server.registerStar()
        signIn(server.loginStar())

        val location = postApi(Api.Locations.CreateLocation, locationEdit()).toDataOrThrow()

        assertEquals(TestDefault.username, location.scout, "the location should name its creator")
        assertEquals(1, locationCount(), "one create should leave one location")
        assertEquals(1, editCount(location.locationId, EditType.Create), "the create should be logged")
    }

    @Test
    fun `a user cannot update a location hosted by someone else`() = runApiTest {
        server.seedCity()
        val hostId = server.registerStar()
        signIn(server.loginStar())
        val location = postApi(Api.Locations.CreateLocation, locationEdit()).toDataOrThrow()
        setHost(location.locationId, hostId)

        server.registerStar(OTHER_USERNAME, OTHER_EMAIL)
        signIn(server.loginStar(OTHER_USERNAME))
        val edit = locationEdit(name = "Renamed Den").copy(locationId = location.locationId)
        postApi(Api.Locations.UpdateLocation, edit).toProblemOrThrow()

        assertEquals(LOCATION_NAME, nameOf(location.locationId), "the host's location should keep its name")
    }

    @Test
    fun `any user can update a location that has no host`() = runApiTest {
        server.seedCity()
        server.registerStar()
        signIn(server.loginStar())
        val location = postApi(Api.Locations.CreateLocation, locationEdit()).toDataOrThrow()

        server.registerStar(OTHER_USERNAME, OTHER_EMAIL)
        signIn(server.loginStar(OTHER_USERNAME))
        val edit = locationEdit(name = "Renamed Den").copy(locationId = location.locationId)
        postApi(Api.Locations.UpdateLocation, edit).toDataOrThrow()

        assertEquals("Renamed Den", nameOf(location.locationId), "a hostless location should accept the update")
    }

    @Test
    fun `only an admin can change a location's config`() = runApiTest {
        server.seedCity()
        server.registerStar()
        signIn(server.loginStar())
        val location = postApi(Api.Locations.CreateLocation, locationEdit()).toDataOrThrow()
        val config = LocationConfig(location.locationId, ParseMode.Full, design = null, subdomain = null)
        val startingMode = parseModeOf(location.locationId)

        postApi(Api.Locations.UpdateConfig, config).toProblemOrThrow()
        assertEquals(startingMode, parseModeOf(location.locationId), "a user should not change the parse mode")

        server.registerAdmin(ADMIN_USERNAME, ADMIN_EMAIL)
        signIn(server.loginStar(ADMIN_USERNAME))
        postApi(Api.Locations.UpdateConfig, config).toDataOrThrow()
        assertEquals(ParseMode.Full, parseModeOf(location.locationId), "an admin should change the parse mode")
    }

    @Test
    fun `a location search returns the locations matching the query`() = runApiTest {
        val starId = registerUser("alice")
        server.seedLocation(starId, "The Fox Den")
        server.seedLocation(starId, "The Owl Nest")

        val found = getApi(Api.Locations.Search) {
            writeParam(it.query, "fox")
            writeParam(it.limit, 10)
        }.toDataOrThrow()

        assertEquals(listOf("The Fox Den"), found.map { it.name }, "only the matching location should be found")
    }

    private fun setHost(locationId: LocationId, hostId: StarId) = transaction {
        LocationTable.update({ LocationTable.id.eq(locationId) }) { it[LocationTable.hostId] = hostId.value }
    }

    private fun locationCount() = transaction { LocationTable.selectAll().count().toInt() }

    private fun nameOf(locationId: LocationId) = transaction {
        LocationTable.selectAll().where { LocationTable.id.eq(locationId) }.single()[LocationTable.name]
    }

    private fun parseModeOf(locationId: LocationId) = transaction {
        LocationTable.selectAll().where { LocationTable.id.eq(locationId) }.single()[LocationTable.parseMode]
    }

    private fun editCount(locationId: LocationId, type: EditType) = transaction {
        EditLogTable.selectAll()
            .where { EditLogTable.recordId.eq(locationId.value) }
            .map { it[EditLogTable.editType] }
            .count { it == type }
    }
}

private val OTHER_USERNAME = Username("otheruser")
private val OTHER_EMAIL = EmailAddress("otheruser@gmail.com")
private val ADMIN_USERNAME = Username("admin1")
private val ADMIN_EMAIL = EmailAddress("admin1@gmail.com")
