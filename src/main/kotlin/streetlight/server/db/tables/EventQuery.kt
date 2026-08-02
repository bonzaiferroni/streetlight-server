package streetlight.server.db.tables

import kampfire.api.toSlug
import kampfire.api.toUsername
import kampfire.model.toUrl
import klutch.db.model.CallerId
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Event
import streetlight.model.data.StarId
import streetlight.server.utils.toRecordId
import streetlight.server.utils.toRecordIdOrNull

fun eventQuery(callerId: CallerId?) = EventTable
    .join(EventStarTable, JoinType.LEFT, EventTable.id, EventStarTable.eventId,
        additionalConstraint = EventStarTable.getConstraint(callerId))
    .select(EventColumns)

val EventColumns = listOf(
    EventTable.id,
    EventTable.locationId,
    EventTable.slug,
    EventTable.scout,
    EventTable.title,
    EventTable.description,
    EventTable.status,
    EventTable.contact,
    EventTable.ageMin,
    EventTable.cost,
    EventTable.visibility,
    EventTable.links,
    EventTable.website,
    EventTable.image,
    EventTable.streamUrl,
    EventTable.timeZoneId,
    EventTable.starCount,
    EventTable.startsAt,
    EventTable.endsAt,
    EventTable.createdAt,
    EventTable.updatedAt,
    EventStarTable.starId,
)

fun ResultRow.toEvent() = Event(
    eventId = toRecordId(EventTable.id),
    locationId = toRecordId(EventTable.locationId),
    currentRequestId = toRecordIdOrNull(EventTable.currentRequestId),
    slug = this[EventTable.slug].toSlug(),
    scout = this[EventTable.scout].toUsername(),
    title = this[EventTable.title],
    description = this[EventTable.description],
    status = this[EventTable.status],
    contact = this[EventTable.contact],
    ageMin = this[EventTable.ageMin],
    cost = this[EventTable.cost],
    visibility = this[EventTable.visibility],
    links = this[EventTable.links],
    website = this[EventTable.website]?.toUrl(),
    image = this[EventTable.image],
    streamUrl = this[EventTable.streamUrl],
    timeZoneId = this[EventTable.timeZoneId],
    lightCount = this[EventTable.starCount],
    isLit = this.getOrNull(EventStarTable.starId) != null,
    startsAt = this[EventTable.startsAt],
    endsAt = this[EventTable.endsAt],
    updatedAt = this[EventTable.updatedAt],
    createdAt = this[EventTable.createdAt],
)