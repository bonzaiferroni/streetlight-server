package streetlight.server.db.tables

import kabinet.utils.toInstantFromUtc
import kabinet.utils.toLocalDateTimeUtc
import klutch.db.tables.UserTable
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.Event
import streetlight.model.data.EventStatus
import streetlight.model.data.EventType
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toProjectIdOrNull
import streetlight.server.utils.toUserId

object EventTable : UUIDTable("event") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val locationId = reference("location_id", LocationTable, onDelete = ReferenceOption.CASCADE)
    val currentRequest = reference("current_song_id", RequestTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val url = text("url").nullable()
    val sourceUrl = text("source_url").nullable()
    val sourceImageUrl = text("source_image_url").nullable()
    val imageUrl = text("image_url").nullable()
    val thumbUrl = text("thumb_url").nullable()
    val streamUrl = text("stream_url").nullable()
    val name = text("name")
    val description = text("description").nullable()
    val status = enumeration<EventStatus>("status")
    val eventType = enumeration<EventType>("event_type")
    val contact = text("contact").nullable()
    val invitation = text("invitation").nullable()
    val ageMin = integer("age_min").nullable()
    val date = date("date")
    val startsAt = datetime("starts_at")
    val endsAt = datetime("ends_at").nullable()
    val updatedAt = datetime("updated_at")
    val createdAt = datetime("created_at")
}

fun ResultRow.toEvent() = Event(
    eventId = toProjectId(EventTable.id),
    userId = toUserId(EventTable.userId),
    locationId = toProjectId(EventTable.locationId),
    currentRequestId = toProjectIdOrNull(EventTable.currentRequest),
    url = this[EventTable.url],
    sourceUrl = this[EventTable.sourceUrl],
    sourceImageUrl = this[EventTable.sourceImageUrl],
    imageUrl = this[EventTable.imageUrl],
    thumbUrl = this[EventTable.thumbUrl],
    streamUrl = this[EventTable.streamUrl],
    title = this[EventTable.name],
    description = this[EventTable.description],
    status = this[EventTable.status],
    eventType = this[EventTable.eventType],
    contact = this[EventTable.contact],
    invitation = this[EventTable.invitation],
    ageMin = this[EventTable.ageMin],
    date = this[EventTable.date],
    startsAt = this[EventTable.startsAt].toInstantFromUtc(),
    endsAt = this[EventTable.endsAt]?.toInstantFromUtc(),
    updatedAt = this[EventTable.updatedAt].toInstantFromUtc(),
    createdAt = this[EventTable.createdAt].toInstantFromUtc()
)

fun UpdateBuilder<*>.writeFull(event: Event) {
    this[EventTable.id] = event.eventId.toUUID()
    this[EventTable.userId] = event.userId.toUUID()
    this[EventTable.locationId] = event.locationId.toUUID()
    this[EventTable.currentRequest] = event.currentRequestId?.toUUID()
    this[EventTable.createdAt] = event.createdAt.toLocalDateTimeUtc()
    writeUpdate(event)
}

fun UpdateBuilder<*>.writeUpdate(event: Event) {
    this[EventTable.url] = event.url
    this[EventTable.sourceUrl] = event.sourceUrl
    this[EventTable.sourceImageUrl] = event.sourceImageUrl
    this[EventTable.imageUrl] = event.imageUrl
    this[EventTable.thumbUrl] = event.thumbUrl
    this[EventTable.streamUrl] = event.streamUrl
    this[EventTable.name] = event.title
    this[EventTable.description] = event.description
    this[EventTable.status] = event.status
    this[EventTable.eventType] = event.eventType
    this[EventTable.contact] = event.contact
    this[EventTable.invitation] = event.invitation
    this[EventTable.ageMin] = event.ageMin
    this[EventTable.date] = event.date
    this[EventTable.startsAt] = event.startsAt.toLocalDateTimeUtc()
    this[EventTable.endsAt] = event.endsAt?.toLocalDateTimeUtc()
    this[EventTable.updatedAt] = event.updatedAt.toLocalDateTimeUtc()
}