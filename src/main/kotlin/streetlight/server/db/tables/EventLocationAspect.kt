package streetlight.server.db.tables

import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.ResultRow
import streetlight.model.data.EventLocation
import streetlight.server.utils.toProjectId

fun ResultRow.toEventLocation() = EventLocation(
    eventId = toProjectId(EventTable.id),
    locationId = toProjectId(EventTable.locationId),
    slug = this[EventTable.slug],
    url = this[EventTable.url],
    images = this[EventTable.images] ?: this[LocationTable.images],
    title = this[EventTable.title],
    description = this[EventTable.description],
    status = this[EventTable.status],
    visibility = (0..20).random(),
    geoPoint = this[LocationTable.geoPoint].toGeoPoint(),
    locationName = this[LocationTable.name],
    startsAt = this[EventTable.startsAt],
    endsAt = this[EventTable.endsAt],
)