package streetlight.server.db.services

import kabinet.console.globalConsole
import kampfire.model.GeoBounds
import kampfire.model.ImageSize
import kampfire.model.UserId
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import streetlight.model.data.Event
import streetlight.model.data.EventId
import streetlight.model.data.EventEdit
import streetlight.model.data.EventStatus
import streetlight.model.data.LightEdit
import streetlight.model.data.LocationId
import streetlight.model.data.Slug
import streetlight.model.data.slugOf
import streetlight.server.db.tables.EventLightTable
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.ImageValues
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.eventInfoQuery
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toEventLocation
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.routes.SaveImageResult
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

    suspend fun readEventBySlug(slug: Slug) = dbQuery {
        EventTable.readFirstOrNull { it.slug.eqLowercase(slug) }?.toEvent()
    }

    suspend fun createEvent(
        userId: UserId,
        event: EventEdit,
        imageValues: ImageValues?
    ) = dbQuery {
        val event = event.toEvent(EventId.random(), userId)
        EventTable.insertWithSlug(event.title, EventTable.slug) {
            it.writeFull(event, imageValues)
        }
        EventTable.readById(event.eventId.toUUID()).toEvent()
    }

    suspend fun updateEvent(
        eventId: EventId,
        userId: UserId,
        edit: EventEdit,
        imageValues: ImageValues?
    ) = dbQuery {
        val event = edit.toEvent(eventId, userId)
        EventTable.updateSingleWhere({ EventTable.userId.eq(userId) and EventTable.id.eq(eventId)}) {
            it.writeUpdate(event, imageValues)
        }
        EventTable.readById(event.eventId.toUUID()).toEvent()
    }

    suspend fun deleteEvent(userId: UserId, eventId: EventId): Boolean = dbQuery {
        EventTable.deleteSingle { EventTable.userId.eq(userId) and EventTable.id.eq(eventId) }
    }

    suspend fun readEventsInBounds(bounds: GeoBounds) = dbQuery { // , after: LocalDate, before: LocalDate
        eventInfoQuery.where { LocationTable.geoPoint.inBounds(bounds) }.map { it.toEventLocation() }
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
        EventTable.leftJoin(LocationTable)
            .selectAll() // td: select only necessary columns
            .where { EventTable.id.inList(eventIds) }
            .map { it.toEventLocation() }
    }

    suspend fun readEventLights(userId: UserId) = dbQuery {
        EventLightTable.read { EventLightTable.UserId.eq(userId) }.map { it[EventLightTable.EventId].toProjectId<EventId>() }
    }

    suspend fun editEventLight(edit: LightEdit, userId: UserId) = dbQuery {
        when (edit.isLit) {
            true -> EventLightTable.insertIgnore {
                it[this.UserId] = userId.toUUID()
                it[this.EventId] = edit.stringId.toUUID()
                it[this.CreatedAt] = Clock.System.now()
            }
            else -> EventLightTable.deleteWhere {
                this.EventId.eq(edit.stringId) and this.UserId.eq(userId)
            }
        }
        true
    }

    suspend fun readImageUrl(eventId: EventId) = dbQuery {
        EventTable.select(EventTable.imageUrl).where { EventTable.id.eq(eventId) }
            .firstOrNull()?.getOrNull(EventTable.imageUrl)
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
    imageSm = null,
    imageMd = null,
    sourceUrl = sourceUrl,
    sourceImageUrl = sourceImageUrl,
    streamUrl = null,
    timeZoneId = timeZoneId ?: error("no time zone"),
    startsAt = startsAt ?: error("no starting time"),
    endsAt = endsAt,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)