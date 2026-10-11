package streetlight.server.integration

import klutch.db.model.CallerId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import streetlight.model.data.EntityCursor
import streetlight.model.data.EventLocation
import streetlight.model.data.EventEdit
import streetlight.model.data.FeedContext
import streetlight.model.data.FeedType
import streetlight.model.data.FeedSource
import streetlight.model.data.Location
import streetlight.model.data.StarId
import streetlight.server.DatabaseTest
import streetlight.server.TestServer
import streetlight.server.model.readCityFeed
import streetlight.server.model.readCityLocationFeed
import streetlight.server.registerStar
import streetlight.server.locationEdit
import streetlight.server.seedLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class CityFeedTest : DatabaseTest() {

    @Test
    fun `paging a city feed returns each upcoming event once, soonest first`() = runTest {
        with(server) {
            val scoutId = registerStar()
            val expected = mutableSetOf<Uuid>()
            repeat(20) { i ->
                val location = seedLocation(scoutId, name = "Venue $i")
                expected += seedEvent(scoutId, location, "Show $i", LocalDate(2030, 3, 1 + i))
                expected += seedEvent(scoutId, location, "Encore $i", LocalDate(2030, 4, 1 + i))
                seedEvent(scoutId, location, "Past Show $i", LocalDate(2020, 3, 14))
            }
            seedLocation(scoutId, name = "Quiet Venue")
            val cityId = dao.city.readCityId("Denver", "Colorado") ?: error("city was not seeded")

            val first = readCityFeed(cityId, null)
            val cursor = first.nextCursor as? EntityCursor.Time ?: error("the first page should lead to a second")
            val second = readCityFeed(cityId, null, cursor)
            val entities = first.entities + second.entities
            val events = entities.filterIsInstance<EventLocation>()
            val seen = events.map { it.eventId.value }

            assertEquals(EntityCursor.DefaultLimit, first.entities.size, "the first page should be full")
            assertNull(second.nextCursor, "the second page should be the last")
            assertEquals(entities.size, events.size, "the feed should hold only events")
            assertEquals(seen.size, seen.toSet().size, "no event should appear twice")
            assertEquals(expected, seen.toSet(), "every upcoming event and no past event should appear")
            assertEquals(events.sortedBy { it.startsAt }, events, "events should come soonest first")
        }
    }

    @Test
    fun `paging a city's locations returns each named location once, newest first`() = runTest {
        with(server) {
            val scoutId = registerStar()
            val expected = (0..EntityCursor.DefaultLimit).map { seedLocation(scoutId, name = "Venue $it").locationId }.toSet()
            val cityId = dao.city.readCityId("Denver", "Colorado") ?: error("city was not seeded")
            dao.location.create(cityId, CallerId(scoutId.value), locationEdit().copy(name = null, timezoneId = "America/Denver"))

            val first = readCityLocationFeed(cityId, null)
            val cursor = first.nextCursor as? EntityCursor.Time ?: error("the first page should lead to a second")
            val second = readCityLocationFeed(cityId, null, cursor)
            val locations = (first.entities + second.entities).map { it as Location }
            val seen = locations.map { it.locationId }

            assertNull(second.nextCursor, "the second page should be the last")
            assertEquals(seen.size, seen.toSet().size, "no location should appear twice")
            assertEquals(expected, seen.toSet(), "every named location of the city, and no unnamed one, should appear")
            val createdTimes = locations.map { it.createdAt }
            assertEquals(createdTimes.sortedDescending(), createdTimes, "locations should come newest first")
            assertEquals(FeedSource(FeedContext.City(cityId), FeedType.Locations), first.source, "the feed should name its source")
        }
    }

    private suspend fun TestServer.seedEvent(scoutId: StarId, location: Location, title: String, date: LocalDate): Uuid {
        val edit = EventEdit(
            title = title,
            locationId = location.locationId,
            date = date,
            startTime = LocalTime(19, 0),
            timeZoneId = "America/Denver",
        )
        val event = dao.event.createEvent(CallerId(scoutId.value), edit) ?: error("event was not created: $title")
        return event.eventId.value
    }
}
