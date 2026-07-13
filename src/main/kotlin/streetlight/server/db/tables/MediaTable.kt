package streetlight.server.db.tables

import kampfire.api.toSlug
import kampfire.api.toUsername
import kampfire.model.CallerId
import kampfire.model.ImageSize
import klutch.db.SyncValueTrigger
import klutch.db.image
import klutch.db.point
import klutch.db.scaledImages
import klutch.db.tables.SlugTable
import klutch.db.url
import klutch.utils.toGeoPoint
import klutch.utils.toPGpoint
import klutch.utils.transformMarkdown
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Media
import streetlight.model.data.MediaType
import streetlight.model.data.StarId
import streetlight.server.utils.toRecordId

object MediaTable: UuidTable("media"), SlugTable {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE)
    override val slug = text("slug").index()
    val username = text("username").transform({ it.toUsername() }, { it.value }) .index()
    val mediaType = enumeration<MediaType>("media_type")
    val title = text("title").nullable()
    val subtitle = text("subtitle").nullable()
    val text = text("text").transformMarkdown().nullable()
    val link = url("link").nullable()
    val geoPoint = point("geo_point").nullable()
    val image = image("image").nullable()
    // td: add unpublished status
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    val imageConfig = imageConfigOf(
        table = this,
        column = image,
        ImageSize.Large,
        ImageSize.Medium,
        ImageSize.Small,
        ImageSize.Thumb,
    )
}

val mediaUsernameSync = SyncValueTrigger(MediaTable.starId, MediaTable.username, StarTable, StarTable.username)

fun ResultRow.toMedia() = Media(
    mediaId = toRecordId(MediaTable.id),
    slug = this[MediaTable.slug].toSlug(),
    username = this[MediaTable.username],
    mediaType = this[MediaTable.mediaType],
    title = this[MediaTable.title],
    subtitle = this[MediaTable.subtitle],
    text = this[MediaTable.text],
    link = this[MediaTable.link],
    geoPoint = this[MediaTable.geoPoint]?.toGeoPoint(),
    image = this[MediaTable.image],
    updatedAt = this[MediaTable.updatedAt],
    createdAt = this[MediaTable.createdAt],
)

fun UpdateBuilder<*>.createRecord(media: Media, callerId: CallerId) {
    this[MediaTable.id] = media.mediaId.value
    this[MediaTable.starId] = callerId.value
    this[MediaTable.mediaType] = media.mediaType
    this[MediaTable.createdAt] = media.createdAt
    writeUpdate(media)
}

fun UpdateBuilder<*>.writeUpdate(media: Media) {
    this[MediaTable.slug] = media.slug.value
    this[MediaTable.title] = media.title
    this[MediaTable.subtitle] = media.subtitle
    this[MediaTable.username] = media.username
    this[MediaTable.text] = media.text
    this[MediaTable.link] = media.link
    this[MediaTable.geoPoint] = media.geoPoint?.toPGpoint()
    this[MediaTable.image] = media.image
    this[MediaTable.updatedAt] = media.updatedAt
    // writeImages(MediaTable.imageConfig, imageSet)
}
