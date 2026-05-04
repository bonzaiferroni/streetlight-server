package streetlight.server.db.services

import kabinet.console.globalConsole
import kampfire.model.GeoBounds
import klutch.db.DbService
import klutch.db.count
import klutch.db.deleteSingle
import klutch.db.inBounds
import klutch.db.inList
import klutch.db.read
import klutch.db.readById
import klutch.db.readFirstOrNull
import klutch.db.updateSingleWhere
import klutch.utils.eq
import klutch.utils.eqLowercase
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.Event
import streetlight.model.data.EventId
import streetlight.model.data.EventEdit
import streetlight.model.data.EventStatus
import streetlight.model.data.LightEdit
import streetlight.model.data.LocationId
import streetlight.model.data.Slug
import streetlight.model.data.StarId
import streetlight.model.data.slugOf
import streetlight.server.db.tables.EventLightTable
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.SavedImageSet
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.EventLocationQuery
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toEventLocation
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.utils.toProjectId
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
        EventTable.readFirstOrNull { it.slug.eqLowercase(slug) }?.toEvent()
    }

    suspend fun readEventLocationBySlug(slug: Slug) = dbQuery {
        EventLocationQuery.where { EventTable.slug.eq(slug) }.firstOrNull()?.toEventLocation()
    }

    suspend fun createEvent(
        starId: StarId,
        event: EventEdit,
        imageSet: SavedImageSet?
    ) = dbQuery {
        val event = event.toEvent(EventId.random())
        EventTable.insertWithSlug(event.title, EventTable.slug) {
            it.writeFull(event, starId, imageSet)
        }
        EventTable.readById(event.eventId.toUUID()).toEvent()
    }

    suspend fun updateEvent(
        eventId: EventId,
        starId: StarId,
        edit: EventEdit,
        imageSet: SavedImageSet?
    ) = dbQuery {
        val event = edit.toEvent(eventId)
        EventTable.updateSingleWhere({ EventTable.starId.eq(starId) and EventTable.id.eq(eventId)}) {
            it.writeUpdate(event, imageSet)
        }
        EventTable.readById(event.eventId.toUUID()).toEvent()
    }

    suspend fun deleteEvent(starId: StarId, eventId: EventId): Boolean = dbQuery {
        EventTable.deleteSingle { EventTable.starId.eq(starId) and EventTable.id.eq(eventId) }
    }

    suspend fun readEventsInBounds(bounds: GeoBounds) = dbQuery { // , after: LocalDate, before: LocalDate
        EventLocationQuery.where { LocationTable.geoPoint.inBounds(bounds) }.map { it.toEventLocation() }
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

    suspend fun readEventLocations(eventIds: List<EventId>) = dbQuery {
        EventLocationQuery.where { EventTable.id.inList(eventIds) }.map { it.toEventLocation() }
    }

    suspend fun readImageUrl(eventId: EventId) = dbQuery {
        EventTable.select(EventTable.imageRef).where { EventTable.id.eq(eventId) }
            .firstOrNull()?.getOrNull(EventTable.imageRef)
    }
}

private fun EventEdit.toEvent(
    eventId: EventId,
) = Event(
    eventId = eventId,
    locationId = locationId ?: error("no location"),
    currentRequestId = null,
    slug = slugOf(title ?: error("no title")),
    title = title ?: error("no title"),
    description = description,
    contact = contact,
    invitation = invitation,
    status = EventStatus.Pending,
    ageMin = ageMin,
    cost = cost ?: error("no cost provided"),
    visibility = null,
    links = links,
    url = link,
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