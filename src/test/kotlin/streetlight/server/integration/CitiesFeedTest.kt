package streetlight.server.integration

import kampfire.api.Slug
import kotlinx.coroutines.test.runTest
import streetlight.model.data.City
import streetlight.model.data.CityId
import streetlight.model.data.EntityCursor
import streetlight.model.data.FeedContext
import streetlight.model.data.FeedSource
import streetlight.model.data.FeedType
import streetlight.server.DENVER
import streetlight.server.DENVER_AREA
import streetlight.server.DatabaseTest
import streetlight.server.TestServer
import streetlight.server.model.readCitiesFeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CitiesFeedTest : DatabaseTest() {

    @Test
    fun `paging the cities returns each city once, by name in any case`() = runTest {
        with(server) {
            val expected = (0..EntityCursor.DefaultLimit).map { seedCityNamed(if (it % 2 == 0) "city $it" else "City $it") }.toSet()

            val first = readCitiesFeed()
            val cursor = first.nextCursor as? EntityCursor.Name ?: error("the first page should lead to a second")
            val second = readCitiesFeed(cursor)
            val cities = (first.entities + second.entities).map { it as City }
            val seen = cities.map { it.cityId }

            assertNull(second.nextCursor, "the second page should be the last")
            assertEquals(seen.size, seen.toSet().size, "no city should appear twice")
            assertEquals(expected, seen.toSet(), "every city should appear")
            val names = cities.map { it.name.lowercase() }
            assertEquals(names.sorted(), names, "cities should come by name, ignoring case")
            assertEquals(FeedSource(FeedContext.Cities, FeedType.Cities), first.source, "the feed should name its source")
        }
    }

    @Test
    fun `searching cities matches the name in any case`() = runTest {
        with(server) {
            val boulder = seedCityNamed("Boulder")
            val southBoulder = seedCityNamed("South BOULDER")
            seedCityNamed("Denver")

            // the feed section sends an upcoming cursor whatever its feed
            val feed = readCitiesFeed(EntityCursor.Upcoming.copy(search = "boulder"))
            val found = feed.entities.map { (it as City).cityId }

            assertEquals(listOf(boulder, southBoulder), found, "only the cities naming boulder should appear, by name")
        }
    }

    private suspend fun TestServer.seedCityNamed(name: String): CityId = dao.city.createCity(
        City(
            cityId = CityId.random(),
            slug = Slug(name.lowercase().replace(' ', '-')),
            name = name,
            state = "Colorado",
            country = "United States",
            galaxyCount = 0,
            locationCount = 0,
            eventCount = 0,
            image = null,
            description = null,
            links = null,
            mapRank = null,
            geoPoint = DENVER,
            geoRect = DENVER_AREA,
        )
    )
}
