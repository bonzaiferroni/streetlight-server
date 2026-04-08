package streetlight.server.db.tables

import kabinet.utils.toInstantFromUtc
import kabinet.utils.toLocalDateTimeUtc
import kampfire.model.ImageSize
import kampfire.model.UserId
import klutch.db.point
import klutch.db.scaledImages
import klutch.db.tables.UserTable
import klutch.db.url
import klutch.utils.toGeoPoint
import klutch.utils.toPGpoint
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.Galaxy
import streetlight.model.data.PostPermission
import streetlight.model.data.ReviewMode
import streetlight.server.utils.toProjectId

object GalaxyTable : UUIDTable("galaxy") {
    val founderId = reference("founder_id", UserTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
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
    val updatedAt = datetime("updated_at")
    val createdAt = datetime("created_at")

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

fun UpdateBuilder<*>.writeGalaxyFull(galaxy: Galaxy, founderId: UserId, imageSet: SavedImageSet?) {
    this[GalaxyTable.id] = galaxy.galaxyId.toUUID()
    this[GalaxyTable.founderId] = founderId.toUUID()
    this[GalaxyTable.createdAt] = galaxy.createdAt.toLocalDateTimeUtc()
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
    this[GalaxyTable.updatedAt] = galaxy.updatedAt.toLocalDateTimeUtc()
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
    updatedAt = this[GalaxyTable.updatedAt].toInstantFromUtc(),
    createdAt = this[GalaxyTable.createdAt].toInstantFromUtc(),
)
