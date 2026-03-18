package streetlight.server.db.services

import kabinet.console.globalConsole
import kampfire.model.GeoBounds
import kampfire.model.UserId
import klutch.db.DbService
import klutch.db.count
import klutch.db.deleteSingle
import klutch.db.inBounds
import klutch.db.read
import klutch.db.readById
import klutch.db.readFirstOrNull
import klutch.db.updateSingleWhere
import klutch.utils.eq
import klutch.utils.toUUID
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.upsert
import streetlight.model.data.Event
import streetlight.model.data.EventId
import streetlight.model.data.EventEdit
import streetlight.model.data.EventStatus
import streetlight.model.data.InterestType
import streetlight.model.data.LocationId
import streetlight.server.db.tables.EventInterestTable
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
        val event = event.toEvent(EventId.random(), userId)
        EventTable.insert {
            it.writeFull(event)
        }
        EventTable.readById(event.eventId.toUUID()).toEvent()
    }

    suspend fun updateEvent(eventId: EventId, userId: UserId, edit: EventEdit) = dbQuery {
        val event = edit.toEvent(eventId, userId)
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

    suspend fun writeUserInterest(eventId: EventId, userId: UserId, interest: InterestType?) = dbQuery {
        if (interest == null) {
            EventInterestTable.deleteWhere {
                EventInterestTable.userId.eq(userId) and EventInterestTable.eventId.eq(eventId)
            } == 1
        } else {
            val statement = EventInterestTable.upsert {
                it[EventInterestTable.userId] = userId.toUUID()
                it[EventInterestTable.eventId] = eventId.toUUID()
                it[EventInterestTable.interest] = interest
                it[EventInterestTable.createdAt] = Clock.System.now()
            }
            statement.insertedCount == 1
        }
    }
}

private fun EventEdit.toEvent(
    eventId: EventId,
    userId: UserId,
) = Event(
    eventId = eventId,
    userId = userId,
    locationId = locationId ?: error("no location"),
    currentRequestId = null,
    contact = contact,
    invitation = invitation,
    streamUrl = null,
    title = title ?: error("no title"),
    description = description,
    status = EventStatus.Pending,
    ageMin = ageMin,
    cost = cost ?: error("no cost provided"),
    visibility = null,
    links = links,
    url = link,
    sourceUrl = sourceUrl,
    sourceImageUrl = sourceImageUrl,
    imageUrl = imageUrl,
    thumbUrl = thumbUrl,
    timeZoneId = timeZoneId ?: error("no time zone"),
    startsAt = startsAt ?: error("no starting time"),
    endsAt = endsAt,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)