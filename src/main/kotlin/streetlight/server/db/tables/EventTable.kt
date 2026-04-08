package streetlight.server.db.tables

import kampfire.model.ImageSize
import klutch.db.tables.UserTable
import klutch.db.url
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.Event
import streetlight.model.data.EventStatus
import streetlight.model.data.ExtraLink
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toProjectIdOrNull
import streetlight.server.utils.toUserId

object EventTable : UUIDTable("event") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val locationId = reference("location_id", LocationTable, onDelete = ReferenceOption.CASCADE)
    val currentRequestId = reference("current_song_id", RequestTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val slug = text("slug").uniqueIndex(SLUG_INDEX)
    val title = text("title")
    val description = text("description").nullable()
    val status = enumeration<EventStatus>("status")
    val contact = text("contact").nullable()
    val invitation = text("invitation").nullable()
    val ageMin = integer("age_min").nullable()
    val cost = float("cost")
    val visibility = integer("visibility").nullable()
    val links = jsonb<List<ExtraLink>>("links", tableJsonDefault).nullable()
    val url = text("url").nullable()
    val sourceUrl = text("source_url").nullable()
    val sourceImageUrl = text("source_image_url").nullable()
    val imageUrl = url("image_url").nullable()
    val imageMd = url("image_md").nullable()
    val imageSm = url("image_sm").nullable()
    val streamUrl = text("stream_url").nullable()
    val timeZoneId = text("time_zone_id")
    // val doorsAt = timestamp("doors_at").nullable()
    val startsAt = timestamp("starts_at")
    val endsAt = timestamp("ends_at").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    const val SLUG_INDEX = "EVENT_SLUG_INDEX"

    val imageConfig = TableImageConfig(
        table = this,
        refColumn = imageUrl,
        sizeColumns = listOf(ImageColumnConfig(imageMd, ImageSize.Medium), ImageColumnConfig(imageSm, ImageSize.Small))
    )
}

fun ResultRow.toEvent() = Event(
    eventId = toProjectId(EventTable.id),
    userId = toUserId(EventTable.userId),
    locationId = toProjectId(EventTable.locationId),
    currentRequestId = toProjectIdOrNull(EventTable.currentRequestId),
    slug = this[EventTable.slug],
    title = this[EventTable.title],
    description = this[EventTable.description],
    status = this[EventTable.status],
    contact = this[EventTable.contact],
    invitation = this[EventTable.invitation],
    ageMin = this[EventTable.ageMin],
    cost = this[EventTable.cost],
    visibility = this[EventTable.visibility],
    links = this[EventTable.links],
    url = this[EventTable.url],
    sourceUrl = this[EventTable.sourceUrl],
    sourceImageUrl = this[EventTable.sourceImageUrl],
    imageMd = this[EventTable.imageMd],
    imageSm = this[EventTable.imageSm],
    streamUrl = this[EventTable.streamUrl],
    timeZoneId = this[EventTable.timeZoneId],
    startsAt = this[EventTable.startsAt],
    endsAt = this[EventTable.endsAt],
    updatedAt = this[EventTable.updatedAt],
    createdAt = this[EventTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(event: Event, imageValues: ImageValues?) {
    this[EventTable.id] = event.eventId.toUUID()
    this[EventTable.userId] = event.userId.toUUID()
    this[EventTable.locationId] = event.locationId.toUUID()
    this[EventTable.currentRequestId] = event.currentRequestId?.toUUID()
    this[EventTable.slug] = event.slug
    this[EventTable.createdAt] = event.createdAt
    writeUpdate(event, imageValues)
}

fun UpdateBuilder<*>.writeUpdate(event: Event, imageValues: ImageValues?) {
    this[EventTable.url] = event.url
    this[EventTable.sourceUrl] = event.sourceUrl
    this[EventTable.sourceImageUrl] = event.sourceImageUrl
    this[EventTable.streamUrl] = event.streamUrl
    this[EventTable.title] = event.title
    this[EventTable.description] = event.description
    this[EventTable.status] = event.status
    this[EventTable.contact] = event.contact
    this[EventTable.invitation] = event.invitation
    this[EventTable.ageMin] = event.ageMin
    this[EventTable.cost] = event.cost
    this[EventTable.visibility] = event.visibility
    this[EventTable.links] = event.links
    this[EventTable.timeZoneId] = event.timeZone.id
    this[EventTable.startsAt] = event.startsAt
    this[EventTable.endsAt] = event.endsAt
    this[EventTable.updatedAt] = event.updatedAt
    writeImages(EventTable.imageConfig, imageValues)
}

val eventInfoQuery get() = EventTable.leftJoin(LocationTable).select(listOf(
    EventTable.id,
    EventTable.locationId,
    EventTable.url,
    EventTable.imageMd,
    EventTable.imageSm,
    LocationTable.imageUrl,
    LocationTable.thumbUrl,
    EventTable.title,
    EventTable.description,
    EventTable.status,
    EventTable.startsAt,
    EventTable.endsAt,
    LocationTable.geoPoint,
))