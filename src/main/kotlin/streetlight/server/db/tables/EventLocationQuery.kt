package streetlight.server.db.tables

import kampfire.api.toSlug
import kampfire.api.toUsername
import klutch.db.model.CallerId
import klutch.utils.eq
import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.EventLocation
import streetlight.server.utils.toRecordId

fun eventLocationQuery(callerId: CallerId?) = EventTable
    .join(LocationTable, JoinType.LEFT, EventTable.locationId, LocationTable.id)
    .join(EventStarTable, JoinType.LEFT, EventTable.id, EventStarTable.eventId,
        additionalConstraint = getConstraint(callerId) { EventStarTable.starId.eq(it) })
    .select(EventLocationColumns)

val EventLocationColumns = listOf(
    EventTable.id,
    EventTable.locationId,
    EventTable.slug,
    EventTable.scout,
    EventTable.website,
    EventTable.image,
    EventTable.title,
    EventTable.description,
    EventTable.cost,
    EventTable.status,
    EventTable.startsAt,
    EventTable.endsAt,
    EventTable.links,
    EventTable.starCount,
    EventTable.createdAt,
    EventTable.updatedAt,
    EventStarTable.starId,
    LocationTable.slug,
    LocationTable.image,
    LocationTable.name,
    LocationTable.description,
    LocationTable.address,
    LocationTable.city,
    LocationTable.website,
    LocationTable.geoPoint,
)

fun ResultRow.toEventLocation() = EventLocation(
    eventId = toRecordId(EventTable.id),
    locationId = toRecordId(EventTable.locationId),
    eventSlug = this[EventTable.slug].toSlug(),
    locationSlug = this[LocationTable.slug].toSlug(),
    scout = this[EventTable.scout].toUsername(),
    url = this[EventTable.website],
    eventImage = this[EventTable.image],
    title = this[EventTable.title],
    description = this[EventTable.description],
    cost = this[EventTable.cost],
    status = this[EventTable.status],
    visibility = (0..20).random(),
    geoPoint = this[LocationTable.geoPoint].toGeoPoint(),
    eventLinks = this[EventTable.links],
    locationName = this[LocationTable.name],
    locationDescription = this[LocationTable.description],
    address = this[LocationTable.address],
    city = this[LocationTable.city],
    locationImage = this[LocationTable.image],
    lightCount = this[EventTable.starCount],
    isLit = this.getOrNull(EventStarTable.starId) != null,
    startsAt = this[EventTable.startsAt],
    endsAt = this[EventTable.endsAt],
    updatedAt = this[EventTable.updatedAt],
    createdAt = this[EventTable.createdAt],
)