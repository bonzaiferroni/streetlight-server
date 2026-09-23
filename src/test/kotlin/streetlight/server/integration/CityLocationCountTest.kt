package streetlight.server.integration

import kampfire.api.Slug
import kampfire.model.GeoPoint
import kampfire.model.GeoRect
import klutch.db.model.CallerId
import kotlinx.coroutines.test.runTest
import klutch.utils.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import streetlight.model.data.City
import streetlight.model.data.CityId
import streetlight.server.DatabaseTest
import streetlight.server.TestServer
import streetlight.server.db.tables.LocationTable
import streetlight.server.locationEdit
import streetlight.server.registerStar
import streetlight.server.seedLocation
import kotlin.test.Test
import kotlin.test.assertEquals

class CityLocationCountTest : DatabaseTest() {

    @Test
    fun `a location created in a city is counted by that city`() = runTest {
        with(server) {
            val location = seedLocation(registerStar())

            assertEquals(1, locationCountOf(location.cityId!!), "the city should count its new location")
        }
    }

    @Test
    fun `a deleted location is no longer counted by its city`() = runTest {
        with(server) {
            val scoutId = registerStar()
            val kept = seedLocation(scoutId)
            val deleted = seedLocation(scoutId, name = "The Owl Roost")

            transaction { LocationTable.deleteWhere { LocationTable.id.eq(deleted.locationId) } }

            assertEquals(1, locationCountOf(kept.cityId!!), "the city should count only the remaining location")
        }
    }

    @Test
    fun `a location moved to another city is counted by the new city only`() = runTest {
        with(server) {
            val scoutId = registerStar()
            val location = seedLocation(scoutId)
            val denverId = location.cityId!!
            val boulderId = seedBoulder()

            val edit = locationEdit().copy(locationId = location.locationId, timezoneId = "America/Denver")
            dao.location.update(location.locationId, boulderId, CallerId(scoutId.value), edit)
                ?: error("location was not updated")

            assertEquals(0, locationCountOf(denverId), "the old city should no longer count the location")
            assertEquals(1, locationCountOf(boulderId), "the new city should count the location")
        }
    }

    private suspend fun TestServer.locationCountOf(cityId: CityId) =
        (dao.city.readCity(cityId) ?: error("city not found: $cityId")).locationCount

    private suspend fun TestServer.seedBoulder() = dao.city.createCity(
        City(
            cityId = CityId.random(),
            slug = Slug("boulder-colorado"),
            name = "Boulder",
            state = "Colorado",
            country = "United States",
            galaxyCount = 0,
            locationCount = 0,
            eventCount = 0,
            image = null,
            mapRank = null,
            geoPoint = GeoPoint(-105.27, 40.01),
            geoRect = GeoRect(GeoPoint(-105.3, 39.95), GeoPoint(-105.2, 40.1)),
        )
    )
}
