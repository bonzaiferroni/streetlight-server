package streetlight.server.db.tables

import kampfire.model.ImageSize
import klutch.db.scaledImages
import klutch.db.url
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.Event
import streetlight.model.data.EventStatus
import streetlight.model.data.ExtraLink
import streetlight.model.data.StarId
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toProjectIdOrNull

object EventTable : UUIDTable("event") {
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE)
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
    val website = text("url").nullable()
    val sourceUrl = text("source_url").nullable()
    val sourceImageUrl = text("source_image_url").nullable()
    val imageRef = url("image_ref").nullable()
    val images = scaledImages("image_array").nullable()
    val streamUrl = text("stream_url").nullable()
    val timeZoneId = text("time_zone_id")
    // val doorsAt = timestamp("doors_at").nullable()
    val startsAt = timestamp("starts_at")
    val endsAt = timestamp("ends_at").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    const val SLUG_INDEX = "event_slug_index"

    val imageConfig = imageConfigOf(
        table = this,
        refColumn = imageRef,
        arrayColumn = images,
        ImageSize.Medium,
        ImageSize.Small,
        ImageSize.Thumb,
    )
}

fun UpdateBuilder<*>.writeFull(event: Event, starId: StarId, imageSet: SavedImageSet?) {
    this[EventTable.id] = event.eventId.toUUID()
    this[EventTable.starId] = starId.toUUID()
    this[EventTable.locationId] = event.locationId.toUUID()
    this[EventTable.currentRequestId] = event.currentRequestId?.toUUID()
    this[EventTable.slug] = event.slug
    this[EventTable.createdAt] = event.createdAt
    writeUpdate(event, imageSet)
}

fun UpdateBuilder<*>.writeUpdate(event: Event, imageSet: SavedImageSet?) {
    this[EventTable.website] = event.url
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
    writeImages(EventTable.imageConfig, imageSet)
}

fun ResultRow.toEvent() = Event(
    eventId = toProjectId(EventTable.id),
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
    url = this[EventTable.website],
    sourceUrl = this[EventTable.sourceUrl],
    sourceImageUrl = this[EventTable.sourceImageUrl],
    imageRef = this[EventTable.imageRef],
    images = this[EventTable.images],
    streamUrl = this[EventTable.streamUrl],
    timeZoneId = this[EventTable.timeZoneId],
    startsAt = this[EventTable.startsAt],
    endsAt = this[EventTable.endsAt],
    updatedAt = this[EventTable.updatedAt],
    createdAt = this[EventTable.createdAt]
)