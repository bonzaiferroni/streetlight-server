package streetlight.server.api

import klutch.db.model.CallerId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import streetlight.model.Api
import streetlight.model.data.EntityCursor
import streetlight.model.data.EventEdit
import streetlight.model.data.EventLocation
import streetlight.model.data.EventTag
import streetlight.model.data.LocationId
import streetlight.model.writeTimeCursor
import streetlight.server.TestServer
import streetlight.server.seedLocation
import streetlight.server.toDataOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class EventFeedApiTest : ApiTest() {

    @Test
    fun `the event feed filtered by a tag holds only events with that tag, across pages`() = runApiTest {
        val scoutId = registerUser("scout")
        val location = server.seedLocation(scoutId)
        val tagged = mutableSetOf<Uuid>()
        repeat(EntityCursor.DefaultLimit + 1) { i ->
            tagged += server.seedEvent(scoutId.value, location.locationId, "Concert $i", LocalDate(2030, 1, 1 + i % 28), EventTag.Concert)
        }
        repeat(3) { i ->
            server.seedEvent(scoutId.value, location.locationId, "Hike $i", LocalDate(2030, 1, 1 + i), EventTag.Hike)
        }
        server.seedEvent(scoutId.value, location.locationId, "Untagged", LocalDate(2030, 1, 2), null)

        val cursor = EntityCursor.Upcoming.copy(tag = EventTag.Concert.ordinal)
        val first = getApi(Api.Events.ReadFeed) { writeTimeCursor(it, cursor) }.toDataOrThrow()
        val next = first.nextCursor as? EntityCursor.Time ?: error("the first page should lead to a second")
        val second = getApi(Api.Events.ReadFeed) { writeTimeCursor(it, next) }.toDataOrThrow()
        val seen = (first.entities + second.entities).map { (it as EventLocation).eventId.value }

        assertEquals(EventTag.Concert.ordinal, next.tag, "the next page should keep the tag")
        assertEquals(seen.size, seen.toSet().size, "no event should appear twice")
        assertEquals(tagged, seen.toSet(), "every concert and nothing else should appear")
    }

    private suspend fun TestServer.seedEvent(
        scoutId: Uuid,
        locationId: LocationId,
        title: String,
        date: LocalDate,
        tag: EventTag?,
    ): Uuid {
        val edit = EventEdit(
            title = title,
            locationId = locationId,
            date = date,
            startTime = LocalTime(19, 0),
            timeZoneId = "America/Denver",
            tags = listOfNotNull(tag),
        )
        val event = dao.event.createEvent(CallerId(scoutId), edit) ?: error("event was not created: $title")
        return event.eventId.value
    }
}
