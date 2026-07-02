package streetlight.server.db.tables

import kampfire.api.toSlug
import kampfire.api.toUsername
import kampfire.model.ImageSize
import klutch.db.SyncValueTrigger
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
import streetlight.model.data.Medium
import streetlight.model.data.MediaType
import streetlight.model.data.StarId
import streetlight.server.utils.toRecordId

object MediumTable: UuidTable("medium"), SlugTable {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE)
    override val slug = text("slug").index()
    val username = text("username").transform({ it.toUsername() }, { it.value }) .index()
    val mediaType = enumeration<MediaType>("media_type")
    val title = text("title").nullable()
    val subtitle = text("subtitle").nullable()
    val text = text("text").transformMarkdown().nullable()
    val link = url("link").nullable()
    val geoPoint = point("geo_point").nullable()
    val imageRef = url("image_ref").nullable()
    val images = scaledImages("images").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

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

val mediumUsernameTrigger = SyncValueTrigger(MediumTable.starId, MediumTable.username, StarTable, StarTable.username)

fun ResultRow.toMedium() = Medium(
    mediumId = toRecordId(MediumTable.id),
    slug = this[MediumTable.slug].toSlug(),
    username = this[MediumTable.username],
    mediaType = this[MediumTable.mediaType],
    title = this[MediumTable.title],
    subtitle = this[MediumTable.subtitle],
    text = this[MediumTable.text],
    link = this[MediumTable.link],
    geoPoint = this[MediumTable.geoPoint]?.toGeoPoint(),
    imageRef = this[MediumTable.imageRef],
    images = this[MediumTable.images],
    updatedAt = this[MediumTable.updatedAt],
    createdAt = this[MediumTable.createdAt],
)

fun UpdateBuilder<*>.createRecord(medium: Medium, callerId: StarId) {
    this[MediumTable.id] = medium.mediumId.value
    this[MediumTable.starId] = callerId.value
    this[MediumTable.mediaType] = medium.mediaType
    this[MediumTable.createdAt] = medium.createdAt
    writeUpdate(medium)
}

fun UpdateBuilder<*>.writeUpdate(medium: Medium) {
    this[MediumTable.slug] = medium.slug.value
    this[MediumTable.title] = medium.title
    this[MediumTable.subtitle] = medium.subtitle
    this[MediumTable.username] = medium.username
    this[MediumTable.text] = medium.text
    this[MediumTable.link] = medium.link
    this[MediumTable.geoPoint] = medium.geoPoint?.toPGpoint()
    this[MediumTable.imageRef] = medium.imageRef
    this[MediumTable.images] = medium.images
    this[MediumTable.updatedAt] = medium.updatedAt
    // writeImages(MediaTable.imageConfig, imageSet)
}
