package streetlight.server.db.tables

import kampfire.api.Slug
import kampfire.api.toSlug
import kampfire.model.GeoPoint
import kampfire.model.ImageSize
import kampfire.model.ScaledImageArray
import kampfire.model.Url
import klutch.db.CounterTrigger
import klutch.db.SyncValueTrigger
import klutch.db.point
import klutch.db.scaledImages
import klutch.db.tables.SlugTable
import klutch.db.url
import klutch.utils.toGeoPoint
import klutch.utils.toPGpoint
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
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
import streetlight.server.utils.toRecordId
import kotlin.time.Instant

object PostTable : UuidTable("post"), SlugTable {
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE).index()
    val starId = reference("user_id", StarTable.id, onDelete = ReferenceOption.SET_NULL).index().nullable()
    val eventId = reference("event_id", EventTable.id, onDelete = ReferenceOption.CASCADE).index().nullable()
    val locationId = reference("location_id", LocationTable.id, onDelete = ReferenceOption.CASCADE).index().nullable()
    override val slug = text("slug").uniqueIndex()
    override val pastSlug = text("past_slug").uniqueIndex().nullable()
    val title = text("title").nullable().index()
    val subtitle = text("subtitle").nullable()
    val username = text("username").nullable()
    val text = text("text").nullable()
    val geoPoint = point("geo_point").nullable()
    val postType = enumeration<PostType>("post_type")
    val imageRef = url("image_ref").nullable()
    val images = scaledImages("images").nullable()
    val links = jsonb<List<ExtraLink>>("links", tableJsonDefault).nullable()
    val updatedAt = timestamp("updated_at").index()
    val createdAt = timestamp("created_at").index()

    // denormalized columns
    val galaxySlug = text("galaxy_slug").nullable()
    val galaxyName = text("galaxy_name").nullable()
    val starCount = integer("star_count").default(0).index()

    val imageConfig = imageConfigOf(
        table = this,
        refColumn = imageRef,
        arrayColumn = images,
        ImageSize.Large,
        ImageSize.Medium,
        ImageSize.Small,
        ImageSize.Thumb,
    )
}

val postStarCountTrigger = CounterTrigger(PostTable, PostStarTable, PostStarTable.postId, PostTable.starCount)
val postUsernameTrigger = SyncValueTrigger(PostTable.starId, PostTable.username, StarTable, StarTable.username)
val postEventLocationTrigger = SyncValueTrigger(PostTable.eventId, PostTable.locationId, EventTable, EventTable.locationId)
val postGalaxyNameTrigger = SyncValueTrigger(PostTable.galaxyId, PostTable.galaxyName, GalaxyTable, GalaxyTable.name)
val postGalaxySlugTrigger = SyncValueTrigger(PostTable.galaxyId, PostTable.galaxySlug, GalaxyTable, GalaxyTable.slug)

fun ResultRow.toPostRecord() = PostRecord(
    postId = this[PostTable.id].toRecordId(),
    galaxyId = this[PostTable.galaxyId].toRecordId(),
    starId = this[PostTable.starId]?.toRecordId(),
    eventId = this[PostTable.eventId]?.toRecordId(),
    locationId = this[PostTable.locationId]?.toRecordId(),
    slug = this[PostTable.slug].toSlug(),
    pastSlug = this[PostTable.pastSlug]?.toSlug(),
    title = this[PostTable.title],
    subtitle = this[PostTable.subtitle],
    text = this[PostTable.text],
    geoPoint = this[PostTable.geoPoint]?.toGeoPoint(),
    lightCount = this[PostTable.starCount],
    imageRef = this[PostTable.imageRef],
    images = this[PostTable.images],
    postType = this[PostTable.postType],
    updatedAt = this[PostTable.updatedAt],
    createdAt = this[PostTable.createdAt],
)

fun UpdateBuilder<*>.createRecord(post: PostRecord, imageSet: SavedImageSet?) {
    this[PostTable.id] = post.postId.value
    this[PostTable.galaxyId] = post.galaxyId.value
    this[PostTable.starId] = post.starId?.value
    this[PostTable.eventId] = post.eventId?.value
    this[PostTable.locationId] = post.locationId?.value
    this[PostTable.postType] = post.postType
    this[PostTable.createdAt] = post.createdAt
    updateRecord(post, imageSet)
}

fun UpdateBuilder<*>.updateRecord(post: PostRecord, imageSet: SavedImageSet?) {
    this[PostTable.slug] = post.slug.string
    this[PostTable.pastSlug] = post.pastSlug?.string
    this[PostTable.title] = post.title
    this[PostTable.subtitle] = post.subtitle
    this[PostTable.text] = post.text
    this[PostTable.geoPoint] = post.geoPoint?.toPGpoint()
    this[PostTable.updatedAt] = post.updatedAt
    writeImages(PostTable.imageConfig, imageSet)
}

@Serializable
data class PostRecord(
    val postId: PostId,
    val galaxyId: GalaxyId,
    val eventId: EventId?,
    val locationId: LocationId?,
    val starId: StarId?,
    val slug: Slug,
    val pastSlug: Slug?,
    val title: String?,
    val subtitle: String?,
    val text: String?,
    val geoPoint: GeoPoint?,
    val lightCount: Int,
    val imageRef: Url?,
    val images: ScaledImageArray?,
    val postType: PostType,
    val updatedAt: Instant,
    val createdAt: Instant,
)