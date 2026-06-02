package streetlight.server.db.services

import kabinet.console.globalConsole
import kampfire.api.Slug
import kampfire.model.GeoBounds
import klutch.db.DbService
import klutch.db.count
import klutch.db.deleteSingle
import klutch.db.inBounds
import klutch.db.inList
import klutch.db.read
import klutch.db.readFirstOrNull
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.Event
import streetlight.model.data.EventId
import streetlight.model.data.EventEdit
import streetlight.model.data.EventStatus
import streetlight.model.data.LocationId
import streetlight.model.data.StarId
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.SavedImageSet
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.eventLocationQuery
import klutch.db.tables.SlugRecord
import klutch.db.tables.getSlugRecord
import klutch.db.tables.nextSlugOf
import org.jetbrains.exposed.v1.jdbc.insert
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toEventLocation
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.updateRecord
import kotlin.time.Clock
import kotlin.time.Instant

private val console = globalConsole.getHandle(EventTableDao::class)

class EventTableDao: DbService() {
    suspend fun readActiveEvents() = dbQuery {
        EventTable.read { it.status.neq(EventStatus.Finished) }
            .map { it.toEvent() }
    }

    suspend fun readEvent(eventId: EventId) = dbQuery {
        EventTable.read { it.id.eq(eventId) }.firstOrNull()?.toEvent()
    }

    suspend fun readEventTitle(eventId: EventId) = dbQuery {
        EventTable.select(EventTable.title).where { EventTable.id.eq(eventId) }.firstOrNull()?.getOrNull(EventTable.title)
    }

    suspend fun readEventBySlug(slug: Slug) = dbQuery {
        EventTable.readFirstOrNull { it.slug.eq(slug) }?.toEvent()
    }

    suspend fun readEventLocationBySlug(slug: Slug, starId: StarId?) = dbQuery {
        eventLocationQuery(starId).where { EventTable.slug.eq(slug) }.firstOrNull()?.toEventLocation()
    }

    suspend fun createEvent(
        starId: StarId,
        edit: EventEdit,
        imageSet: SavedImageSet?
    ) = dbQuery {
        val eventId = EventId.random()
        val title = edit.title ?: error("title not found")
        val slug = EventTable.nextSlugOf(title)
        val event = edit.toEvent(eventId)
        EventTable.insert {
            it.createRecord(event, starId, SlugRecord(slug), imageSet)
        }
        readEvent(eventId)
    }

    suspend fun updateEvent(
        eventId: EventId,
        starId: StarId,
        edit: EventEdit,
        imageSet: SavedImageSet?
    ) = dbQuery {
        val title = edit.title ?: error("title not found")
        val slugSync = EventTable.getSlugRecord(eventId, title)
        val event = edit.toEvent(eventId)
        EventTable.update({ EventTable.starId.eq(starId) and EventTable.id.eq(eventId)}) {
            it.updateRecord(event, slugSync, imageSet)
        }
        readEvent(eventId)
    }

    suspend fun deleteEvent(starId: StarId, eventId: EventId): Boolean = dbQuery {
        EventTable.deleteSingle { EventTable.starId.eq(starId) and EventTable.id.eq(eventId) }
    }

    suspend fun readEventsInBounds(bounds: GeoBounds, starId: StarId?) = dbQuery { // , after: LocalDate, before: LocalDate
        eventLocationQuery(starId).where { LocationTable.geoPoint.inBounds(bounds) }.map { it.toEventLocation() }
    }

    suspend fun hasConflict(edit: EventEdit) = dbQuery {
        val title = edit.title ?: error("no title")
        val startsAt = edit.startsAt ?: error("no time")
        val locationId = edit.locationId ?: return@dbQuery false
        // td: more precise time conflict handling
        EventTable.count {
            EventTable.locationId.eq(locationId) and EventTable.startsAt.eq(startsAt) and EventTable.title.eq(title)
        } > 0
    }

    suspend fun readLocationEvents(locationId: LocationId) = dbQuery {
        EventTable.read { it.locationId.eq(locationId) }.map { it.toEvent() }
    }

    suspend fun readEventAt(locationId: LocationId, startsAt: Instant) = dbQuery {
        EventTable.readFirstOrNull { it.locationId.eq(locationId) and it.startsAt.eq(startsAt) }?.toEvent()
    }

    suspend fun readEventLocations(eventIds: List<EventId>, starId: StarId?) = dbQuery {
        eventLocationQuery(starId).where { EventTable.id.inList(eventIds) }.map { it.toEventLocation() }
    }

    suspend fun readImageUrl(eventId: EventId) = dbQuery {
        EventTable.select(EventTable.imageRef).where { EventTable.id.eq(eventId) }
            .firstOrNull()?.getOrNull(EventTable.imageRef)
    }
}

private fun EventEdit.toEvent(eventId: EventId) = Event(
    eventId = eventId,
    locationId = locationId ?: error("no location"),
    currentRequestId = null,
    slug = Slug.Empty,
    title = title ?: error("no title"),
    description = description,
    contact = contact,
    invitation = invitation,
    status = EventStatus.Pending,
    ageMin = ageMin,
    cost = cost ?: error("no cost provided"),
    visibility = null,
    links = links,
    url = url,
    imageRef = imageRef,
    images = null,
    sourceUrl = sourceUrl,
    sourceImageUrl = sourceImageUrl,
    streamUrl = null,
    timeZoneId = timeZoneId ?: error("no time zone"),
    startsAt = startsAt ?: error("no starting time"),
    endsAt = endsAt,
    lightCount = 0,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)