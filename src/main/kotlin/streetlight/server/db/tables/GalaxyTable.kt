package streetlight.server.db.tables

import kampfire.model.ImageSize
import klutch.db.point
import klutch.db.scaledImages
import klutch.db.url
import klutch.utils.toGeoPoint
import klutch.utils.toPGpoint
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Galaxy
import streetlight.model.data.PostPermission
import streetlight.model.data.ReviewMode
import streetlight.model.data.StarId
import streetlight.server.utils.toProjectId

object GalaxyTable : UUIDTable("galaxy") {
    val founderId = reference("founder_id", StarTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val path = text("path").uniqueIndex()
    val name = text("name")
    val description = text("description").nullable()
    val center = point("center")
    val zoom = float("zoom")
    val postPermission = enumeration<PostPermission>("post_permission")
    val reviewMode = enumeration<ReviewMode>("review_mode")
    val postGuide = text("post_guide").nullable()
    val imageRef = url("image_ref").nullable()
    val images = scaledImages("images").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    val lightCount = GalaxyLightTable.starId.count()
    val eventCount = PostTable.eventId.count()

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

fun UpdateBuilder<*>.writeGalaxyFull(galaxy: Galaxy, founderId: StarId, imageSet: SavedImageSet?) {
    this[GalaxyTable.id] = galaxy.galaxyId.toUUID()
    this[GalaxyTable.founderId] = founderId.toUUID()
    this[GalaxyTable.createdAt] = galaxy.createdAt
    writeGalaxyUpdate(galaxy, imageSet)
}

fun UpdateBuilder<*>.writeGalaxyUpdate(galaxy: Galaxy, imageSet: SavedImageSet?) {
    this[GalaxyTable.name] = galaxy.name
    this[GalaxyTable.description] = galaxy.description
    this[GalaxyTable.path] = galaxy.slug
    this[GalaxyTable.center] = galaxy.center.toPGpoint()
    this[GalaxyTable.zoom] = galaxy.zoom
    this[GalaxyTable.postPermission] = galaxy.postPermission
    this[GalaxyTable.reviewMode] = galaxy.reviewMode
    this[GalaxyTable.postGuide] = galaxy.postGuide
    this[GalaxyTable.updatedAt] = galaxy.updatedAt
    writeImages(GalaxyTable.imageConfig, imageSet)
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
    imageRef = this[GalaxyTable.imageRef],
    images = this[GalaxyTable.images],
    lightCount = this.getOrNull(GalaxyTable.lightCount)?.toInt(),
    eventCount = this.getOrNull(GalaxyTable.eventCount)?.toInt(),
    locationCount = this.getOrNull(GalaxyTable.eventCount)?.toInt(),
    updatedAt = this[GalaxyTable.updatedAt],
    createdAt = this[GalaxyTable.createdAt],
)
