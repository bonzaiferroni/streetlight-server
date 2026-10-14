package streetlight.server.integration

import klutch.db.model.CallerId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import streetlight.model.data.EntityCursor
import streetlight.model.data.EventEdit
import streetlight.model.data.EventLocation
import streetlight.model.data.FeedContext
import streetlight.model.data.FeedType
import streetlight.model.data.FeedSource
import streetlight.model.data.Location
import streetlight.model.data.StarId
import streetlight.server.DatabaseTest
import streetlight.server.TestServer
import streetlight.server.service.readEventFeed
import streetlight.server.service.readHomeContent
import streetlight.server.registerStar
import streetlight.server.seedLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class UpcomingEventFeedTest : DatabaseTest() {

    @Test
    fun `paging the event feed returns each upcoming event once, soonest first`() = runTest {
        with(server) {
            val scoutId = registerStar()
            val location = seedLocation(scoutId)
            val expected = mutableSetOf<Uuid>()
            // pairs share a start, so the order between them rests on the id
            repeat(20) { i ->
                val date = LocalDate(2030, 1, 1 + i)
                expected += seedEvent(scoutId, location, "Early Show $i", date)
                expected += seedEvent(scoutId, location, "Late Show $i", date)
            }
            seedEvent(scoutId, location, "Past Show", LocalDate(2020, 3, 14))

            val first = readEventFeed()
            val cursor = first.nextCursor as? EntityCursor.Time ?: error("the first page should lead to a second")
            val second = readEventFeed(cursor)
            val events = (first.entities + second.entities).map { it as EventLocation }
            val seen = events.map { it.eventId.value }

            assertEquals(EntityCursor.DefaultLimit, first.entities.size, "the first page should be full")
            assertNull(second.nextCursor, "the second page should be the last")
            assertEquals(seen.size, seen.toSet().size, "no event should appear twice")
            assertEquals(expected, seen.toSet(), "every upcoming event and no past event should appear")
            assertEquals(events.sortedBy { it.startsAt }, events, "events should come soonest first")
            assertEquals(FeedSource(FeedContext.Home, FeedType.Events), first.source, "the feed should name its source")
        }
    }

    @Test
    fun `the home feed holds events without a caller and the posts of followed galaxies with one`() = runTest {
        with(server) {
            val starId = registerStar()
            val visitorSource = readHomeContent(null).feed.source
            val starSource = readHomeContent(CallerId(starId.value)).feed.source

            assertEquals(FeedSource(FeedContext.Home, FeedType.Events), visitorSource, "a visitor should see events")
            assertEquals(FeedSource(FeedContext.Home, FeedType.GalaxyPosts), starSource, "a star should see galaxy posts")
        }
    }

    @Test
    fun `a star who chooses events on the home page sees events, and a visitor is not offered galaxy posts`() = runTest {
        with(server) {
            val starId = registerStar()
            val starFeed = readHomeContent(CallerId(starId.value), FeedType.Events).feed
            val visitorFeed = readHomeContent(null, FeedType.GalaxyPosts).feed

            assertEquals(FeedSource(FeedContext.Home, FeedType.Events), starFeed.source, "the star should see the type they chose")
            assertEquals(FeedContext.Home.types, starFeed.types, "a star should be offered every home type")
            assertEquals(FeedSource(FeedContext.Home, FeedType.Events), visitorFeed.source, "a visitor asking for galaxy posts should see events")
            assertEquals(listOf(FeedType.Events, FeedType.Media), visitorFeed.types, "a visitor should be offered events and media")
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
