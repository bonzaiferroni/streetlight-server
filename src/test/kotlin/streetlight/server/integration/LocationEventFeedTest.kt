package streetlight.server.integration

import klutch.db.model.CallerId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import streetlight.model.data.EventEdit
import streetlight.model.data.EventLocation
import streetlight.model.data.FeedContext
import streetlight.model.data.FeedType
import streetlight.model.data.FeedSource
import streetlight.model.data.Location
import streetlight.model.data.StarId
import streetlight.server.DatabaseTest
import streetlight.server.TestServer
import streetlight.server.model.readLocationEventFeed
import streetlight.server.registerStar
import streetlight.server.seedLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.Uuid

class LocationEventFeedTest : DatabaseTest() {

    @Test
    fun `a location's feed holds its upcoming events and those under way, and no others`() = runTest {
        with(server) {
            val scoutId = registerStar()
            val location = seedLocation(scoutId, name = "Mercury Cafe")
            val elsewhere = seedLocation(scoutId, name = "Open Air Stage")
            val expected = setOf(
                seedEventAt(scoutId, location, "Under Way", (-2).hours),
                seedEventAt(scoutId, location, "Tomorrow", 1.days),
            )
            seedEventAt(scoutId, location, "Long Over", (-8).hours)
            seedEventAt(scoutId, elsewhere, "Elsewhere", 1.days)

            val feed = readLocationEventFeed(location.locationId, null)
            val seen = feed.entities.map { (it as EventLocation).eventId.value }

            val source = FeedSource(FeedContext.Location(location.locationId), FeedType.Events)
            assertEquals(expected, seen.toSet(), "the events under way and upcoming at the location should appear")
            assertEquals(source, feed.source, "the feed should name its source")
        }
    }

    private suspend fun TestServer.seedEventAt(scoutId: StarId, location: Location, title: String, fromNow: Duration): Uuid {
        val start = (Clock.System.now() + fromNow).toLocalDateTime(TimeZone.of("America/Denver"))
        val edit = EventEdit(
            title = title,
            locationId = location.locationId,
            date = start.date,
            startTime = LocalTime(start.hour, start.minute),
            timeZoneId = "America/Denver",
        )
        val event = dao.event.createEvent(CallerId(scoutId.value), edit) ?: error("event was not created: $title")
        return event.eventId.value
    }
}
