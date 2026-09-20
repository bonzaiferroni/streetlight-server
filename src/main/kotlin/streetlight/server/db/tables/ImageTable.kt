package streetlight.server.db.tables

import kampfire.model.ImageVariant
import kampfire.model.toUrl
import klutch.db.jsonbConfig
import klutch.db.url
import koala.Image
import koala.ImageId
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.*
import streetlight.server.utils.toRecordId

object ImageTable : UuidTable("image") {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE).nullable()
    val url = url("url")
    val format = enumeration<ImageFormat>("format")
    val name = text("name").nullable()
    val aspect = float("aspect")
    val description = text("description").nullable()
    val attribution = text("attribution").nullable()
    val attributionUrl = text("attributionUrl").nullable()
    val caption = text("string").nullable()
    val variants = jsonb<List<ImageVariant>>("variants", jsonbConfig).nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toImageRecord() = ImageRecord(
    imageId = ImageId(this[ImageTable.id].value),
    starId = this[ImageTable.starId]?.toRecordId(),
    image = toImage(),
    format = this[ImageTable.format],
    updatedAt = this[ImageTable.updatedAt],
    createdAt = this[ImageTable.createdAt]
)

fun UpdateBuilder<*>.createImage(record: ImageRecord) {
    this[ImageTable.id] = record.imageId.value
    this[ImageTable.starId] = record.starId?.value
    this[ImageTable.createdAt] = record.createdAt
    updateImage(record)
}

fun UpdateBuilder<*>.updateImage(record: ImageRecord) {
    this[ImageTable.url] = record.image.url
    this[ImageTable.format] = record.format
    this[ImageTable.variants] = record.image.variants
    this[ImageTable.name] = record.image.name
    this[ImageTable.aspect] = record.image.aspect ?: error("aspect not found")
    this[ImageTable.description] = record.image.description
    this[ImageTable.attribution] = record.image.attribution
    this[ImageTable.attributionUrl] = record.image.attributionUrl?.value
    this[ImageTable.caption] = record.image.caption
    this[ImageTable.updatedAt] = record.updatedAt
}

fun ResultRow.toImage() = Image(
    url = this[ImageTable.url],
    variants = this[ImageTable.variants],
    name = this[ImageTable.name],
    aspect = this[ImageTable.aspect],
    description = this[ImageTable.description],
    attribution = this[ImageTable.attribution],
    attributionUrl = this[ImageTable.attributionUrl]?.toUrl(),
    caption = this[ImageTable.caption],
)