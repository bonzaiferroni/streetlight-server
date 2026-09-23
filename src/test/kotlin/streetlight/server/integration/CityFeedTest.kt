package streetlight.server.integration

import kampfire.api.Slug
import klutch.db.model.CallerId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import streetlight.model.data.EntityCursor
import streetlight.model.data.EventLocation
import streetlight.model.data.EventEdit
import streetlight.model.data.Location
import streetlight.model.data.StarId
import streetlight.server.DatabaseTest
import streetlight.server.TestServer
import streetlight.server.db.services.cityRecordId
import streetlight.server.model.readCityFeed
import streetlight.server.registerStar
import streetlight.server.seedLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class CityFeedTest : DatabaseTest() {

    @Test
    fun `paging a city feed returns each location and event once`() = runTest {
        with(server) {
            val scoutId = registerStar()
            val expected = mutableSetOf<Uuid>()
            repeat(20) { i ->
                val location = seedLocation(scoutId, name = "Venue $i")
                val eventId = seedEvent(scoutId, location, "Show $i", LocalDate(2020, 3, 14))
                expected += location.locationId.value
                expected += eventId
            }
            val cityId = dao.city.readCityId("Denver", "Colorado") ?: error("city was not seeded")

            val first = readCityFeed(cityId, null)
            val cursor = first.nextCursor as? EntityCursor.Time ?: error("the first page should lead to a second")
            val second = readCityFeed(cityId, null, cursor)
            val entities = first.entities + second.entities
            val seen = entities.map { it.cityRecordId }

            assertEquals(EntityCursor.DefaultLimit, first.entities.size, "the first page should be full")
            assertNull(second.nextCursor, "the second page should be the last")
            assertEquals(seen.size, seen.toSet().size, "no entity should appear twice")
            assertEquals(expected, seen.toSet(), "every location and event should appear")
            assertEquals(20, entities.count { it is Location }, "each location should be its own entity")
            assertEquals(20, entities.count { it is EventLocation }, "each event should be its own entity")
        }
    }

    @Test
    fun `a city map holds its locations and only upcoming events`() = runTest {
        with(server) {
            val scoutId = registerStar()
            val location = seedLocation(scoutId)
            val upcomingId = seedEvent(scoutId, location, "Upcoming Show", LocalDate(2030, 3, 14))
            seedEvent(scoutId, location, "Past Show", LocalDate(2020, 3, 14))

            val entities = dao.city.readCityPosts(Slug("denver-colorado"), null)

            assertEquals(
                setOf(location.locationId.value, upcomingId),
                entities.map { it.cityRecordId }.toSet(),
                "the map should hold the location and its upcoming event",
            )
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
