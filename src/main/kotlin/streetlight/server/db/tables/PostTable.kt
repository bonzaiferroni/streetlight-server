package streetlight.server.db.tables

import kampfire.model.GeoPoint
import kampfire.model.ImageSize
import kampfire.model.ScaledImageArray
import kampfire.model.Url
import klutch.db.point
import klutch.db.scaledImages
import klutch.db.url
import klutch.utils.toGeoPoint
import klutch.utils.toPGpoint
import klutch.utils.toUUID
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.EventId
import streetlight.model.data.ExtraLink
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
    val geoPoint = point("geo_point").nullable()
    val postType = enumeration<PostType>("post_type")
    val imageRef = url("image_ref").nullable()
    val images = scaledImages("images").nullable()
    val links = jsonb<List<ExtraLink>>("links", tableJsonDefault).nullable()
    val updatedAt = timestamp("updated_at").index()
    val createdAt = timestamp("created_at").index()

    // td: use partial index
    //         CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_location_per_galaxy
    //            ON posts (location_id, galaxy_id)
    //            WHERE post_type = 'location'
    // init {
    //     uniqueIndex("galaxy_event_index", galaxyId, eventId)
    // }

    val imageConfig = imageConfigOf(
        table = this,
        refColumn = imageRef,
        arrayColumn = images,
        ImageSize.Medium,
        ImageSize.Small,
        ImageSize.Thumb,
    )
}

fun ResultRow.toPostRow() = PostRow(
    postId = this[PostTable.id].toProjectId(),
    galaxyId = this[PostTable.galaxyId].toProjectId(),
    starId = this[PostTable.starId]?.toProjectId(),
    eventId = this[PostTable.eventId]?.toProjectId(),
    locationId = this[PostTable.locationId]?.toProjectId(),
    title = this[PostTable.title],
    text = this[PostTable.text],
    geoPoint = this[PostTable.geoPoint]?.toGeoPoint(),
    imageRef = this[PostTable.imageRef],
    images = this[PostTable.images],
    postType = this[PostTable.postType],
    updatedAt = this[PostTable.updatedAt],
    createdAt = this[PostTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(post: PostRow, imageSet: SavedImageSet?) {
    this[PostTable.id] = post.postId.toUUID()
    this[PostTable.galaxyId] = post.galaxyId.toUUID()
    this[PostTable.starId] = post.starId?.toUUID()
    this[PostTable.eventId] = post.eventId?.toUUID()
    this[PostTable.locationId] = post.locationId?.toUUID()
    this[PostTable.postType] = post.postType
    this[PostTable.createdAt] = post.createdAt
    writeUpdate(post, imageSet)
}

fun UpdateBuilder<*>.writeUpdate(post: PostRow, imageSet: SavedImageSet?) {
    this[PostTable.title] = post.title
    this[PostTable.text] = post.text
    this[PostTable.geoPoint] = post.geoPoint?.toPGpoint()
    this[PostTable.images] = post.images
    this[PostTable.updatedAt] = post.updatedAt
    writeImages(PostTable.imageConfig, imageSet)
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
    val geoPoint: GeoPoint?,
    val imageRef: Url?,
    val images: ScaledImageArray?,
    val postType: PostType,
    val updatedAt: Instant,
    val createdAt: Instant,
)