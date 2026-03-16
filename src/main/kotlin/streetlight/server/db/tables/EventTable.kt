package streetlight.server.db.tables

import kabinet.utils.toInstantFromUtc
import kabinet.utils.toLocalDateTimeUtc
import klutch.db.tables.UserTable
import klutch.utils.toGeoPoint
import klutch.utils.toUUID
import kotlinx.datetime.TimeZone
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.Event
import streetlight.model.data.EventInfo
import streetlight.model.data.EventStatus
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toProjectIdOrNull
import streetlight.server.utils.toUserId

object EventTable : UUIDTable("event") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val locationId = reference("location_id", LocationTable, onDelete = ReferenceOption.CASCADE)
    val currentRequest = reference("current_song_id", RequestTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val title = text("title")
    val description = text("description").nullable()
    val status = enumeration<EventStatus>("status")
    val contact = text("contact").nullable()
    val invitation = text("invitation").nullable()
    val ageMin = integer("age_min").nullable()
    val cost = float("cost")
    val visibility = integer("visibility").nullable()
    val url = text("url").nullable()
    val sourceUrl = text("source_url").nullable()
    val sourceImageUrl = text("source_image_url").nullable()
    val imageUrl = text("image_url").nullable()
    val thumbUrl = text("thumb_url").nullable()
    val streamUrl = text("stream_url").nullable()
    val timeZoneId = text("time_zone_id")
    // val doorsAt = timestamp("doors_at").nullable()
    val startsAt = timestamp("starts_at")
    val endsAt = timestamp("ends_at").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toEvent() = Event(
    eventId = toProjectId(EventTable.id),
    userId = toUserId(EventTable.userId),
    locationId = toProjectId(EventTable.locationId),
    currentRequestId = toProjectIdOrNull(EventTable.currentRequest),
    title = this[EventTable.title],
    description = this[EventTable.description],
    status = this[EventTable.status],
    contact = this[EventTable.contact],
    invitation = this[EventTable.invitation],
    ageMin = this[EventTable.ageMin],
    cost = this[EventTable.cost],
    visibility = this[EventTable.visibility],
    url = this[EventTable.url],
    sourceUrl = this[EventTable.sourceUrl],
    sourceImageUrl = this[EventTable.sourceImageUrl],
    imageUrl = this[EventTable.imageUrl],
    thumbUrl = this[EventTable.thumbUrl],
    streamUrl = this[EventTable.streamUrl],
    timeZoneId = this[EventTable.timeZoneId],
    startsAt = this[EventTable.startsAt],
    endsAt = this[EventTable.endsAt],
    updatedAt = this[EventTable.updatedAt],
    createdAt = this[EventTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(event: Event) {
    this[EventTable.id] = event.eventId.toUUID()
    this[EventTable.userId] = event.userId.toUUID()
    this[EventTable.locationId] = event.locationId.toUUID()
    this[EventTable.currentRequest] = event.currentRequestId?.toUUID()
    this[EventTable.createdAt] = event.createdAt
    writeUpdate(event)
}

fun UpdateBuilder<*>.writeUpdate(event: Event) {
    this[EventTable.url] = event.url
    this[EventTable.sourceUrl] = event.sourceUrl
    this[EventTable.sourceImageUrl] = event.sourceImageUrl
    this[EventTable.imageUrl] = event.imageUrl
    this[EventTable.thumbUrl] = event.thumbUrl
    this[EventTable.streamUrl] = event.streamUrl
    this[EventTable.title] = event.title
    this[EventTable.description] = event.description
    this[EventTable.status] = event.status
    this[EventTable.contact] = event.contact
    this[EventTable.invitation] = event.invitation
    this[EventTable.ageMin] = event.ageMin
    this[EventTable.cost] = event.cost
    this[EventTable.timeZoneId] = event.timeZone.id
    this[EventTable.startsAt] = event.startsAt
    this[EventTable.endsAt] = event.endsAt
    this[EventTable.updatedAt] = event.updatedAt
}

val eventInfoQuery get() = EventTable.leftJoin(LocationTable).select(listOf(
    EventTable.id,
    EventTable.locationId,
    EventTable.url,
    EventTable.imageUrl,
    LocationTable.imageUrl,
    EventTable.thumbUrl,
    LocationTable.thumbUrl,
    EventTable.title,
    EventTable.description,
    EventTable.status,
    EventTable.startsAt,
    EventTable.endsAt,
    LocationTable.geoPoint,
))

fun ResultRow.toEventInfo() = EventInfo(
    eventId = toProjectId(EventTable.id),
    locationId = toProjectId(EventTable.locationId),
    url = this[EventTable.url],
    imageUrl = this[EventTable.imageUrl] ?: this[LocationTable.imageUrl],
    thumbUrl = this[EventTable.thumbUrl] ?: this[LocationTable.thumbUrl],
    title = this[EventTable.title],
    description = this[EventTable.description],
    status = this[EventTable.status],
    visibility = (0..20).random(),
    startsAt = this[EventTable.startsAt],
    endsAt = this[EventTable.endsAt],
    geoPoint = this[LocationTable.geoPoint].toGeoPoint(),
)