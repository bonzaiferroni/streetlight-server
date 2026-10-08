package streetlight.server.db.services

import kabinet.console.globalConsole
import kampfire.api.Slug
import kampfire.api.Username
import kampfire.model.GeoRect
import klutch.db.DbService
import klutch.db.count
import klutch.db.deleteSingle
import klutch.db.inRect
import klutch.db.model.CallerId
import klutch.db.read
import klutch.db.readFirstOrNull
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import streetlight.model.data.EntityCursor
import streetlight.model.data.SortDirection
import streetlight.server.db.tables.EventLocationAspect
import streetlight.server.db.tables.joinWith
import streetlight.server.db.tables.joinEventStar
import streetlight.server.db.tables.selectEventStar
import streetlight.model.data.CityId
import streetlight.server.db.tables.selectWith
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.Event
import streetlight.model.data.EventId
import streetlight.model.data.EventEdit
import streetlight.model.data.EventStatus
import streetlight.model.data.LocationId
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.eventLocationQuery
import klutch.db.tables.SlugRecord
import klutch.db.tables.getSlugRecord
import klutch.db.tables.nextSlugOf
import klutch.utils.inList
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.insert
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toEventLocation
import streetlight.server.db.tables.createEvent
import streetlight.server.db.tables.eventQuery
import streetlight.server.db.tables.updateEvent
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val console = globalConsole.getHandle(EventTableDao::class)

class EventTableDao: DbService() {
    suspend fun createEvent(
        callerId: CallerId?,
        edit: EventEdit,
    ) = dbQuery {
        val eventId = EventId.random()
        val title = edit.title ?: error("title not found")
        val slug = EventTable.nextSlugOf(title)
        val event = edit.toEvent(eventId)
        EventTable.insert {
            it.createEvent(event, callerId.takeIf { edit.isHost == true }, SlugRecord(slug))
        }
        readEvent(eventId, callerId)
    }

    suspend fun updateEvent(
        eventId: EventId,
        callerId: CallerId,
        edit: EventEdit,
    ) = dbQuery {
        val title = edit.title ?: error("title not found")
        val slugSync = EventTable.getSlugRecord(eventId, title)
        val event = edit.toEvent(eventId)
        EventTable.update({ EventTable.id.eq(eventId) and (EventTable.hostId.isNull() or EventTable.hostId.eq(callerId)) }) {
            it.updateEvent(event, slugSync)
        }
        readEvent(eventId, callerId)
    }

    /** Whether the caller hosts the event, or it has no host. */
    suspend fun canEdit(eventId: EventId, callerId: CallerId) = dbQuery {
        EventTable.selectAll()
            .where { EventTable.id.eq(eventId) and (EventTable.hostId.isNull() or EventTable.hostId.eq(callerId)) }
            .limit(1)
            .any()
    }

    /** Whether an event with the same title starts at the same time at the same location. */
    suspend fun hasConflict(edit: EventEdit) = dbQuery {
        val title = edit.title ?: error("no title")
        val startsAt = edit.startsAt ?: error("no time")
        val locationId = edit.locationId ?: return@dbQuery false
        // td: more precise time conflict handling
        EventTable.count {
            EventTable.locationId.eq(locationId) and EventTable.startsAt.eq(startsAt) and EventTable.title.eq(title)
        } > 0
    }

    /** Deletes an event the caller hosts, or any event for an admin. */
    suspend fun deleteEvent(callerId: CallerId, eventId: EventId, isAdmin: Boolean): Boolean = dbQuery {
        val mayDelete: Op<Boolean> = if (isAdmin) Op.TRUE else EventTable.hostId.eq(callerId)
        EventTable.deleteSingle { mayDelete and EventTable.id.eq(eventId) }
    }

    suspend fun readEventsInBounds(bounds: GeoRect, callerId: CallerId?) = dbQuery { // , after: LocalDate, before: LocalDate
        eventLocationQuery(callerId).where { LocationTable.geoPoint.inRect(bounds) }.map { it.toEventLocation() }
    }

    suspend fun readActiveEvents() = dbQuery {
        EventTable.read { it.status.neq(EventStatus.Finished) }
            .map { it.toEvent() }
    }

    suspend fun readEvent(eventId: EventId, callerId: CallerId?) = dbQuery {
        eventQuery(callerId).where { EventTable.id.eq(eventId) }.firstOrNull()?.toEvent()
    }

    suspend fun readEvent(slug: Slug, callerId: CallerId?) = dbQuery {
        eventQuery(callerId).where { EventTable.slug.eq(slug) }.firstOrNull()?.toEvent()
    }

    suspend fun readEventTitle(eventId: EventId) = dbQuery {
        EventTable.select(EventTable.title).where { EventTable.id.eq(eventId) }.firstOrNull()?.getOrNull(EventTable.title)
    }

    suspend fun readEventBySlug(slug: Slug) = dbQuery {
        EventTable.readFirstOrNull { it.slug.eq(slug) }?.toEvent()
    }

    suspend fun readEventLocationBySlug(slug: Slug, callerId: CallerId?) = dbQuery {
        eventLocationQuery(callerId).where { EventTable.slug.eq(slug) }.firstOrNull()?.toEventLocation()
    }

    suspend fun readLocationEvents(slug: Slug, callerId: CallerId?) = dbQuery {
        val startsAt = Clock.System.now() - 6.hours
        eventQuery(callerId).where {
            EventTable.locationSlug.eq(slug) and EventTable.startsAt.isNotNull() and EventTable.startsAt.greaterEq(startsAt)
        }
            .orderBy(EventTable.startsAt, SortOrder.ASC)
            .map { it.toEvent() }
    }

    /** The events at the location starting within [from] until [until]. */
    suspend fun readEventsBetween(locationId: LocationId, from: Instant, until: Instant) = dbQuery {
        EventTable.read {
            it.locationId.eq(locationId) and it.startsAt.greaterEq(from) and it.startsAt.less(until)
        }.map { it.toEvent() }
    }

    suspend fun readEventAt(locationId: LocationId, startsAt: Instant) = dbQuery {
        EventTable.readFirstOrNull { it.locationId.eq(locationId) and it.startsAt.eq(startsAt) }?.toEvent()
    }

    suspend fun readEventLocations(eventIds: List<EventId>, callerId: CallerId?) = dbQuery {
        eventLocationQuery(callerId).where { EventTable.id.inList(eventIds) }.map { it.toEventLocation() }
    }

    suspend fun readImageUrl(eventId: EventId) = dbQuery {
        EventTable.select(EventTable.image).where { EventTable.id.eq(eventId) }
            .firstOrNull()?.getOrNull(EventTable.image)
    }

    /**
     * The page of upcoming events after [cursor], in the order of their start, as seen by [callerId].
     *
     * Limited to the events in [cityId] when given.
     */
    suspend fun readUpcomingEvents(
        cursor: EntityCursor.Time,
        callerId: CallerId? = null,
        cityId: CityId? = null,
    ) = dbQuery {
        EventTable.joinWith(LocationTable)
            .joinEventStar(callerId)
            .selectWith(EventLocationAspect.columns) {
                selectEventStar(callerId)
            }
            .where { EventTable.startsAt.greaterEq(Clock.System.now()) }
            .whereInCity(cityId)
            .whereAfterStart(cursor)
            .orderByStart(cursor)
            .limit(EntityCursor.DefaultLimit)
            .map { it.toEventLocation() }
    }
}

/** Filters events to those at locations in [cityId], or leaves them all without one. */
private fun Query.whereInCity(cityId: CityId?): Query =
    cityId?.let { andWhere { LocationTable.cityId.eq(it) } } ?: this

/** Filters events to those after [cursor] by their start. */
private fun Query.whereAfterStart(cursor: EntityCursor.Time): Query {
    val recordId = cursor.recordId ?: return this
    val recordAt = cursor.recordAt ?: return this
    return andWhere {
        when (cursor.direction) {
            SortDirection.Descending -> EventTable.startsAt.less(recordAt) or
                (EventTable.startsAt.eq(recordAt) and EventTable.id.less(recordId))
            SortDirection.Ascending -> EventTable.startsAt.greater(recordAt) or
                (EventTable.startsAt.eq(recordAt) and EventTable.id.greater(recordId))
        }
    }
}

/** Orders events as [whereAfterStart] pages them. */
private fun Query.orderByStart(cursor: EntityCursor.Time): Query {
    val order = when (cursor.direction) {
        SortDirection.Descending -> SortOrder.DESC
        SortDirection.Ascending -> SortOrder.ASC
    }
    return orderBy(EventTable.startsAt to order, EventTable.id to order)
}

private fun EventEdit.toEvent(eventId: EventId) = Event(
    eventId = eventId,
    locationId = locationId ?: error("no location"),
    currentRequestId = null,
    slug = Slug.Empty, // set with trigger
    host = Username.Empty, // set with trigger
    title = title ?: error("no title"),
    description = description,
    tags = tags,
    contact = contact,
    status = EventStatus.Pending,
    ageMin = ageMin,
    cost = cost,
    visibility = null,
    links = links,
    url = website,
    image = image,
    streamUrl = null,
    isLit = false, // set with join
    timeZoneId = timeZoneId ?: error("no time zone"),
    startsAt = startsAt,
    endsAt = endsAt,
    lightCount = 0,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)