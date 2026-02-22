package streetlight.server.db.services

import kabinet.console.globalConsole
import kampfire.model.UserId
import klutch.db.DbService
import klutch.db.deleteSingle
import klutch.db.isNearEq
import klutch.db.read
import klutch.db.readFirstOrNull
import klutch.db.updateSingleWhere
import klutch.utils.eq
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import streetlight.model.data.Event
import streetlight.model.data.EventId
import streetlight.model.data.EventEdit
import streetlight.model.data.EventStatus
import streetlight.model.data.LocationId
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toLocation
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.utils.toProjectId

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
        val now = Clock.System.now()
        val initialLocationId = event.locationId
        val locationId = if (initialLocationId != null) {
            initialLocationId
        } else {
            val newLocation = event.newLocation
            if (newLocation == null || !newLocation.isValid) return@dbQuery null
            val location = LocationTable.readFirstOrNull {
                it.name.eq(newLocation.name) and it.address.eq(newLocation.address) and it.geoPoint.isNearEq(newLocation.geoPoint)
            }?.toLocation()

            if (location != null) {
                console.log("Using stored location ${location.name}")
                location.locationId
            } else {
                console.log("creating new location ${newLocation.name} at ${newLocation.address}")
                LocationTable.insertAndGetId {
                    it.writeFull(newLocation.toLocation())
                }.toProjectId()
            }
        }
        val event = event.toEvent(userId, locationId, now, now)
        EventTable.insert { it.writeFull(event) }
        event
    }

    suspend fun updateEvent(userId: UserId, event: Event): Boolean = dbQuery {
        EventTable.updateSingleWhere({ EventTable.userId.eq(userId) and EventTable.id.eq(event.eventId) }) {
            it.writeUpdate(event)
        } != null
    }

    suspend fun deleteEvent(userId: UserId, eventId: EventId): Boolean = dbQuery {
        EventTable.deleteSingle { EventTable.userId.eq(userId) and EventTable.id.eq(eventId) }
    }
}

private fun EventEdit.toEvent(
    userId: UserId,
    locationId: LocationId,
    updatedAt: Instant,
    createdAt: Instant
) = Event(
    eventId = eventId ?: EventId.random(),
    userId = userId,
    locationId = locationId,
    currentRequestId = null,
    url = url,
    sourceUrl = sourceUrl,
    sourceImageUrl = sourceImageUrl,
    imageUrl = imageUrl,
    thumbUrl = thumbUrl,
    contact = contact,
    invitation = invitation,
    ageMin = ageMin,
    streamUrl = null,
    title = title,
    description = description,
    status = EventStatus.Pending,
    eventType = eventType,
    startsAt = startsAt,
    date = date,
    endsAt = null,
    updatedAt = updatedAt,
    createdAt = createdAt,
)