package streetlight.server.db.tables

import kampfire.api.toSlug
import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.ResultRow
import streetlight.model.data.Media
import streetlight.server.utils.toRecordId

val MediaColumns = listOf(
    MediaTable.id,
    MediaTable.slug,
    MediaTable.username,
    MediaTable.mediaType,
    MediaTable.title,
    MediaTable.subtitle,
    MediaTable.text,
    MediaTable.link,
    MediaTable.geoPoint,
    MediaTable.image,
    MediaTable.updatedAt,
    MediaTable.createdAt,
)

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
    design = getOrNull(MediaTable.design),
    updatedAt = this[MediaTable.updatedAt],
    createdAt = this[MediaTable.createdAt],
)