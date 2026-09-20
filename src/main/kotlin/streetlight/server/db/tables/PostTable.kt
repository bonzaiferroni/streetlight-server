package streetlight.server.db.tables

import kampfire.api.Markdown
import klutch.db.CounterTrigger
import klutch.db.SyncValueTrigger
import klutch.utils.transformMarkdown
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.FeedStatus
import streetlight.model.data.EventId
import streetlight.model.data.GalaxyId
import streetlight.model.data.LocationId
import streetlight.model.data.MediaId
import streetlight.model.data.PostId
import streetlight.model.data.PostType
import streetlight.model.data.StarId
import streetlight.server.utils.toRecordId
import kotlin.time.Instant

object PostTable : UuidTable("post") {
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE).index()
    val starId = reference("user_id", StarTable.id, onDelete = ReferenceOption.SET_NULL).index().nullable()
    val eventId = reference("event_id", EventTable.id, onDelete = ReferenceOption.CASCADE).index().nullable()
    val locationId = reference("location_id", LocationTable.id, onDelete = ReferenceOption.CASCADE).index().nullable()
    val mediaId = reference("media_id", MediaTable.id, ReferenceOption.CASCADE).index().nullable()
    val title = text("title").nullable().index()
    val username = text("username").nullable()
    val text = text("text").transformMarkdown().nullable()
    val postType = enumeration<PostType>("post_type")
    val status = enumeration<FeedStatus>("status").default(FeedStatus.Live) // td: remove default value
    val lean = integer("lean").default(0)
    val updatedAt = timestamp("updated_at").index()
    val createdAt = timestamp("created_at").index()

    // denormalized columns
    val galaxySlug = text("galaxy_slug").nullable()
    val galaxyName = text("galaxy_name").nullable()
    val starCount = integer("star_count").default(0).index()
}

val postStarCountTrigger = CounterTrigger(PostTable, PostMarkTable, PostMarkTable.postId, PostTable.starCount)
val postUsernameSync = SyncValueTrigger(PostTable.starId, PostTable.username, StarTable, StarTable.username)
val postEventLocationSync = SyncValueTrigger(PostTable.eventId, PostTable.locationId, EventTable, EventTable.locationId)
val postGalaxyNameSync = SyncValueTrigger(PostTable.galaxyId, PostTable.galaxyName, GalaxyTable, GalaxyTable.name)
val postGalaxySlugSync = SyncValueTrigger(PostTable.galaxyId, PostTable.galaxySlug, GalaxyTable, GalaxyTable.slug)

fun UpdateBuilder<*>.createPost(post: PostRecord, status: FeedStatus) {
    this[PostTable.id] = post.postId.value
    this[PostTable.galaxyId] = post.galaxyId.value
    this[PostTable.starId] = post.starId?.value
    this[PostTable.eventId] = post.eventId?.value
    this[PostTable.locationId] = post.locationId?.value
    this[PostTable.mediaId] = post.mediaId?.value
    this[PostTable.postType] = post.postType
    this[PostTable.status] = status // initial status
    this[PostTable.createdAt] = post.createdAt
    updatePost(post)
}

fun UpdateBuilder<*>.updatePost(post: PostRecord) {
    this[PostTable.title] = post.title
    this[PostTable.text] = post.text
    this[PostTable.updatedAt] = post.updatedAt
}

fun ResultRow.toPostRecord() = PostRecord(
    postId = this[PostTable.id].toRecordId(),
    galaxyId = this[PostTable.galaxyId].toRecordId(),
    starId = this[PostTable.starId]?.toRecordId(),
    eventId = this[PostTable.eventId]?.toRecordId(),
    locationId = this[PostTable.locationId]?.toRecordId(),
    mediaId = this[PostTable.mediaId]?.toRecordId(),
    title = this[PostTable.title],
    text = this[PostTable.text],
    lightCount = this[PostTable.starCount],
    postType = this[PostTable.postType],
    updatedAt = this[PostTable.updatedAt],
    createdAt = this[PostTable.createdAt],
)

@Serializable
data class PostRecord(
    val postId: PostId,
    val galaxyId: GalaxyId,
    val eventId: EventId?,
    val locationId: LocationId?,
    val mediaId: MediaId?,
    val starId: StarId?,
    val title: String?,
    val text: Markdown?,
    val lightCount: Int,
    val postType: PostType,
    val updatedAt: Instant,
    val createdAt: Instant,
)