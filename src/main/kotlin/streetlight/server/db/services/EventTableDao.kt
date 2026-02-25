package streetlight.server.db.services

import kabinet.console.globalConsole
import kampfire.model.GeoBounds
import kampfire.model.UserId
import klutch.db.DbService
import klutch.db.deleteSingle
import klutch.db.inBounds
import klutch.db.read
import klutch.db.readById
import klutch.db.readFirst
import klutch.db.updateSingleWhere
import klutch.utils.eq
import klutch.utils.toUUID
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import streetlight.model.data.Event
import streetlight.model.data.EventId
import streetlight.model.data.EventEdit
import streetlight.model.data.EventStatus
import streetlight.model.data.LocationId
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.eventInfoQuery
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toEventInfo
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate

private val console = globalConsole.getHandle(EventTableDao::class)

class EventTableDao: DbService() {
    suspend fun readActiveEvents() = dbQuery {
        EventTable.read { it.status.neq(EventStatus.Finished) }
            .map { it.toEvent() }
    }

    suspend fun readEvent(eventId: EventId) = dbQuery {
        EventTable.read { it.id.eq(eventId) }.firstOrNull()?.toEvent()
    }

    suspend fun createEvent(userId: UserId, event: EventEdit) = dbQuery {
        val initialLocationId = event.locationId
        val locationId = initialLocationId ?: event.place?.let {
            findOrCreatePlace(it, userId)
        } ?: return@dbQuery null
        val event = event.toEvent(EventId.random(), userId, locationId)
        EventTable.insert {
            it.writeFull(event)
        }
        EventTable.readById(event.eventId.toUUID()).toEvent()
    }

    suspend fun updateEvent(eventId: EventId, userId: UserId, edit: EventEdit) = dbQuery {
        val initialLocationId = edit.locationId
        val locationId = initialLocationId ?: edit.place?.let {
            findOrCreatePlace(it, userId)
        } ?: return@dbQuery null
        val event = edit.toEvent(eventId, userId, locationId)
        EventTable.updateSingleWhere({ EventTable.userId.eq(userId) and EventTable.id.eq(eventId)}) {
            it.writeUpdate(event)
        }
        EventTable.readById(event.eventId.toUUID()).toEvent()
    }

    suspend fun updateEvent(userId: UserId, event: Event): Boolean = dbQuery {
        EventTable.updateSingleWhere({ EventTable.userId.eq(userId) and EventTable.id.eq(event.eventId) }) {
            it.writeUpdate(event)
        } != null
    }

    suspend fun deleteEvent(userId: UserId, eventId: EventId): Boolean = dbQuery {
        EventTable.deleteSingle { EventTable.userId.eq(userId) and EventTable.id.eq(eventId) }
    }

    suspend fun readEventsInBounds(bounds: GeoBounds) = dbQuery { // , after: LocalDate, before: LocalDate
        eventInfoQuery.where { LocationTable.geoPoint.inBounds(bounds) }.map { it.toEventInfo() }
    }
}

private fun EventEdit.toEvent(
    eventId: EventId,
    userId: UserId,
    locationId: LocationId,
) = Event(
    eventId = eventId,
    userId = userId,
    locationId = locationId,
    currentRequestId = null,
    contact = contact,
    invitation = invitation,
    streamUrl = null,
    title = title,
    description = description,
    status = EventStatus.Pending,
    eventType = eventType,
    ageMin = ageMin,
    visibility = null,
    url = url,
    sourceUrl = sourceUrl,
    sourceImageUrl = sourceImageUrl,
    imageUrl = imageUrl,
    thumbUrl = thumbUrl,
    startsAt = startsAt,
    date = date,
    endsAt = null,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)