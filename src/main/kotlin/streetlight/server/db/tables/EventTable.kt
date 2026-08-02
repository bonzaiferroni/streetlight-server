package streetlight.server.db.tables

import kampfire.model.ImageSize
import kampfire.model.toUrl
import klutch.db.CounterTrigger
import klutch.db.SyncValueTrigger
import klutch.db.image
import klutch.db.model.CallerId
import klutch.db.scaledImages
import klutch.db.tables.SlugRecord
import klutch.db.tables.SlugTable
import klutch.utils.transformMarkdown
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.Event
import streetlight.model.data.EventStatus
import streetlight.model.data.ExtraLink
import streetlight.model.data.StarId

object EventTable : UuidTable("event"), SlugTable {
    val scoutId = reference("scout_id", StarTable, onDelete = ReferenceOption.CASCADE)
    val locationId = reference("location_id", LocationTable, onDelete = ReferenceOption.CASCADE)
    val currentRequestId = reference("current_song_id", RequestTable, onDelete = ReferenceOption.SET_NULL).nullable()
    override val slug = text("slug").uniqueIndex()
    override val pastSlug = text("past_slug").index().nullable()
    val scout = text("scout").index().default("")
    val locationSlug = text("location_slug").index().default("")
    val title = text("title")
    val description = text("description").transformMarkdown().nullable()
    val status = enumeration<EventStatus>("status")
    val contact = text("contact").nullable()
    val ageMin = integer("age_min").nullable()
    val cost = float("cost")
    val visibility = integer("visibility").nullable()
    val links = jsonb<List<ExtraLink>>("links", tableJsonDefault).nullable()
    val website = text("url").nullable()
    val image = image("image").nullable()
    val streamUrl = text("stream_url").nullable()
    val timeZoneId = text("time_zone_id")
    val starCount = integer("star_count").default(0)
    // val doorsAt = timestamp("doors_at").nullable()
    val startsAt = timestamp("starts_at")
    val endsAt = timestamp("ends_at").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    val imageConfig = imageConfigOf(
        table = this,
        column = image,
        ImageSize.Medium,
        ImageSize.Small,
        ImageSize.Thumb,
    )
}

val eventStarCountTrigger = CounterTrigger(EventTable, EventStarTable, EventStarTable.eventId, EventTable.starCount)
val eventLocationSlugSync = SyncValueTrigger(EventTable.locationId, EventTable.locationSlug, LocationTable, LocationTable.slug)
val eventUsernameSync = SyncValueTrigger(EventTable.scoutId, EventTable.scout, StarTable, StarTable.username)

fun UpdateBuilder<*>.createRecord(event: Event, starId: CallerId, slugRecord: SlugRecord) {
    this[EventTable.id] = event.eventId.value
    this[EventTable.scoutId] = starId.value
    this[EventTable.locationId] = event.locationId.value
    this[EventTable.currentRequestId] = event.currentRequestId?.value
    this[EventTable.createdAt] = event.createdAt
    updateRecord(event, slugRecord)
}

fun UpdateBuilder<*>.updateRecord(event: Event, slugRecord: SlugRecord) {
    this[EventTable.slug] = slugRecord.slug.value
    this[EventTable.pastSlug] = slugRecord.pastSlug?.value
    this[EventTable.website] = event.website?.value
    this[EventTable.streamUrl] = event.streamUrl
    this[EventTable.title] = event.title
    this[EventTable.description] = event.description
    this[EventTable.status] = event.status
    this[EventTable.contact] = event.contact
    this[EventTable.ageMin] = event.ageMin
    this[EventTable.cost] = event.cost
    this[EventTable.visibility] = event.visibility
    this[EventTable.links] = event.links
    this[EventTable.image] = event.image
    this[EventTable.timeZoneId] = event.timeZone.id
    this[EventTable.startsAt] = event.startsAt
    this[EventTable.endsAt] = event.endsAt
    this[EventTable.updatedAt] = event.updatedAt
}