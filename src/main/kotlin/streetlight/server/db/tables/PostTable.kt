package streetlight.server.db.tables

import klutch.utils.toUUID
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.EventId
import streetlight.model.data.GalaxyId
import streetlight.model.data.LocationId
import streetlight.model.data.PostId
import streetlight.model.data.PostType
import streetlight.model.data.StarId
import streetlight.server.utils.toProjectId
import kotlin.time.Instant

object PostTable : UUIDTable("post") {
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE)
    val starId = reference("user_id", StarTable.id, onDelete = ReferenceOption.SET_NULL).index().nullable()
    val eventId = reference("event_id", EventTable.id, onDelete = ReferenceOption.CASCADE).index().nullable()
    val locationId = reference("location_id", LocationTable.id, onDelete = ReferenceOption.CASCADE).index().nullable()
    val title = text("title").nullable()
    val text = text("text").nullable()
    val postType = enumeration<PostType>("post_type")
    val updatedAt = timestamp("updated_at").index()
    val createdAt = timestamp("created_at").index()

    // td: use partial index
    //         CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_location_per_galaxy
    //            ON posts (location_id, galaxy_id)
    //            WHERE post_type = 'location'
    // init {
    //     uniqueIndex("galaxy_event_index", galaxyId, eventId)
    // }
}

fun ResultRow.toPostRow() = PostRow(
    postId = this[PostTable.id].toProjectId(),
    galaxyId = this[PostTable.galaxyId].toProjectId(),
    starId = this[PostTable.starId]?.toProjectId(),
    eventId = this[PostTable.eventId]?.toProjectId(),
    locationId = this[PostTable.locationId]?.toProjectId(),
    title = this[PostTable.title],
    text = this[PostTable.text],
    postType = this[PostTable.postType],
    updatedAt = this[PostTable.updatedAt],
    createdAt = this[PostTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(post: PostRow) {
    this[PostTable.id] = post.postId.toUUID()
    this[PostTable.galaxyId] = post.galaxyId.toUUID()
    this[PostTable.starId] = post.starId?.toUUID()
    this[PostTable.eventId] = post.eventId?.toUUID()
    this[PostTable.postType] = post.postType
    this[PostTable.createdAt] = post.createdAt
    writeUpdate(post)
}

fun UpdateBuilder<*>.writeUpdate(post: PostRow) {
    this[PostTable.title] = post.title
    this[PostTable.text] = post.text
    this[PostTable.updatedAt] = post.updatedAt
}

@Serializable
data class PostRow(
    val postId: PostId,
    val galaxyId: GalaxyId,
    val eventId: EventId?,
    val locationId: LocationId?,
    val starId: StarId?,
    val title: String?,
    val text: String?,
    val postType: PostType,
    val updatedAt: Instant,
    val createdAt: Instant,
)