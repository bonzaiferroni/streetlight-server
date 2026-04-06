package streetlight.server.db.tables

import kabinet.utils.toInstantFromUtc
import kabinet.utils.toLocalDateTimeUtc
import klutch.db.point
import klutch.utils.toGeoPoint
import klutch.utils.toPGpoint
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.Galaxy
import streetlight.model.data.PostPermission
import streetlight.model.data.ReviewMode
import streetlight.server.utils.toProjectId

object GalaxyTable : UUIDTable("galaxy") {
    val path = text("path").uniqueIndex()
    val name = text("name")
    val description = text("description").nullable()
    val center = point("center")
    val zoom = float("zoom")
    val postPermission = enumeration<PostPermission>("post_permission")
    val reviewMode = enumeration<ReviewMode>("review_mode")
    val postGuide = text("post_guide").nullable()
    val imageUrl = text("image_url").nullable()
    val thumbUrl = text("thumb_url").nullable()
    val updatedAt = datetime("updated_at")
    val createdAt = datetime("created_at")
}

fun ResultRow.toGalaxy() = Galaxy(
    galaxyId = toProjectId(GalaxyTable.id),
    slug = this[GalaxyTable.path],
    name = this[GalaxyTable.name],
    description = this[GalaxyTable.description],
    center = this[GalaxyTable.center].toGeoPoint(),
    zoom = this[GalaxyTable.zoom],
    postPermission = this[GalaxyTable.postPermission],
    reviewMode = this[GalaxyTable.reviewMode],
    postGuide = this[GalaxyTable.postGuide],
    imageUrl = this[GalaxyTable.imageUrl],
    thumbUrl = this[GalaxyTable.thumbUrl],
    updatedAt = this[GalaxyTable.updatedAt].toInstantFromUtc(),
    createdAt = this[GalaxyTable.createdAt].toInstantFromUtc(),
)

fun UpdateBuilder<*>.writeGalaxyFull(galaxy: Galaxy) {
    this[GalaxyTable.id] = galaxy.galaxyId.toUUID()
    this[GalaxyTable.createdAt] = galaxy.createdAt.toLocalDateTimeUtc()
    writeGalaxyUpdate(galaxy)
}

fun UpdateBuilder<*>.writeGalaxyUpdate(galaxy: Galaxy) {
    this[GalaxyTable.name] = galaxy.name
    this[GalaxyTable.description] = galaxy.description
    this[GalaxyTable.path] = galaxy.slug
    this[GalaxyTable.center] = galaxy.center.toPGpoint()
    this[GalaxyTable.zoom] = galaxy.zoom
    this[GalaxyTable.postPermission] = galaxy.postPermission
    this[GalaxyTable.reviewMode] = galaxy.reviewMode
    this[GalaxyTable.postGuide] = galaxy.postGuide
    this[GalaxyTable.imageUrl] = galaxy.imageUrl
    this[GalaxyTable.thumbUrl] = galaxy.thumbUrl
    this[GalaxyTable.updatedAt] = galaxy.updatedAt.toLocalDateTimeUtc()
}
