package streetlight.server.db.tables

import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.EventLocation
import streetlight.server.utils.toProjectId

val EventLocationQuery get() = EventTable.join(StarTable, JoinType.LEFT, EventTable.starId, StarTable.id)
        .join(LocationTable, JoinType.LEFT, EventTable.locationId, LocationTable.id).select(EventLocationColumns)

val EventLocationColumns = listOf(
    EventTable.id,
    EventTable.locationId,
    EventTable.slug,
    EventTable.website,
    EventTable.images,
    EventTable.title,
    EventTable.description,
    EventTable.cost,
    EventTable.status,
    EventTable.startsAt,
    EventTable.endsAt,
    EventTable.links,
    EventTable.createdAt,
    EventTable.updatedAt,
    StarTable.username,
    LocationTable.images,
    LocationTable.name,
    LocationTable.description,
    LocationTable.address,
    LocationTable.city,
    LocationTable.website,
    LocationTable.geoPoint,
)

fun ResultRow.toEventLocation() = EventLocation(
    eventId = toProjectId(EventTable.id),
    locationId = toProjectId(EventTable.locationId),
    slug = this[EventTable.slug],
    username = this[StarTable.username],
    url = this[EventTable.website],
    eventImages = this[EventTable.images],
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
    locationImages = this[LocationTable.images],
    lightCount = this.getOrNull(EventTable.lightCount)?.toInt(),
    startsAt = this[EventTable.startsAt],
    endsAt = this[EventTable.endsAt],
    updatedAt = this[EventTable.updatedAt],
    createdAt = this[EventTable.createdAt],
)