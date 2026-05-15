package streetlight.server.db.tables

import kampfire.model.ImageSize
import klutch.db.point
import klutch.db.scaledImages
import klutch.db.url
import klutch.utils.toGeoPoint
import klutch.utils.toPGpoint
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.City
import streetlight.model.data.CityId
import streetlight.model.data.Galaxy
import streetlight.model.data.PostPermission
import streetlight.model.data.ReviewMode
import streetlight.model.data.StarId
import streetlight.server.utils.toProjectId

object GalaxyTable : UuidTable("galaxy") {
    val founderId = reference("founder_id", StarTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val cityId = reference("city_id", CityTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val cityName = text("city_name").nullable()
    val slug = text("path").uniqueIndex() // td: rename column as slug
    val name = text("name")
    val tagLine = text("tag_line").nullable()
    val description = text("description").nullable()
    val center = point("center")
    val zoom = float("zoom")
    val postPermission = enumeration<PostPermission>("post_permission")
    val reviewMode = enumeration<ReviewMode>("review_mode")
    val postGuide = text("post_guide").nullable()
    val lightCount = integer("light_count").default(0)
    val imageRef = url("image_ref").nullable()
    val images = scaledImages("images").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    val lightCountSource = GalaxyLightTable.starId.count()
    val eventCountSource = PostTable.eventId.count()

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

fun UpdateBuilder<*>.writeGalaxyFull(galaxy: Galaxy, founderId: StarId, city: City?, imageSet: SavedImageSet?) {
    this[GalaxyTable.id] = galaxy.galaxyId.value
    this[GalaxyTable.founderId] = founderId.value
    this[GalaxyTable.createdAt] = galaxy.createdAt
    writeGalaxyUpdate(galaxy, city, imageSet)
}

fun UpdateBuilder<*>.writeGalaxyUpdate(galaxy: Galaxy, city: City?, imageSet: SavedImageSet?) {
    this[GalaxyTable.cityId] = city?.cityId?.value
    this[GalaxyTable.cityName] = city?.name
    this[GalaxyTable.name] = galaxy.name
    this[GalaxyTable.description] = galaxy.description
    this[GalaxyTable.slug] = galaxy.slug
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
    cityId = this[GalaxyTable.cityId]?.let { CityId(it.value) },
    slug = this[GalaxyTable.slug],
    name = this[GalaxyTable.name],
    tagLine = this[GalaxyTable.tagLine],
    description = this[GalaxyTable.description],
    center = this[GalaxyTable.center].toGeoPoint(),
    zoom = this[GalaxyTable.zoom],
    postPermission = this[GalaxyTable.postPermission],
    reviewMode = this[GalaxyTable.reviewMode],
    postGuide = this[GalaxyTable.postGuide],
    imageRef = this[GalaxyTable.imageRef],
    images = this[GalaxyTable.images],
    lightCount = this[GalaxyTable.lightCount],
    eventCount = this.getOrNull(GalaxyTable.eventCountSource)?.toInt(),
    locationCount = this.getOrNull(GalaxyTable.eventCountSource)?.toInt(),
    updatedAt = this[GalaxyTable.updatedAt],
    createdAt = this[GalaxyTable.createdAt],
)
