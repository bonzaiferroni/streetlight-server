package streetlight.server.db.tables

import kampfire.api.toSlug
import kampfire.model.ImageSize
import klutch.db.CounterTrigger
import klutch.db.point
import klutch.db.scaledImages
import klutch.db.tables.SlugRecord
import klutch.db.tables.SlugTable
import klutch.db.url
import klutch.utils.toGeoBounds
import klutch.utils.toGeoPoint
import klutch.utils.toList
import klutch.utils.toPGpoint
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.City
import streetlight.model.data.CityId
import streetlight.model.data.Galaxy
import streetlight.model.data.PostPermission
import streetlight.model.data.ReviewMode
import streetlight.model.data.StarId
import streetlight.server.utils.toRecordId

object GalaxyTable: UuidTable("galaxy"), SlugTable {
    val founderId = reference("founder_id", StarTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val cityId = reference("city_id", CityTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val city = text("city").nullable()
    override val slug = text("path").uniqueIndex() // td: rename column as slug
    override val pastSlug = text("past_slug").index().nullable()
    val name = text("name")
    val tagline = text("tagline").nullable()
    val description = text("description").nullable()
    val geoPoint = point("geo_point")
    val geoBounds = array<Double>("geo_bounds")
    val postPermission = enumeration<PostPermission>("post_permission")
    val reviewMode = enumeration<ReviewMode>("review_mode")
    val postGuide = text("post_guide").nullable()
    val starCount = integer("star_count").default(0)
    val eventCount = integer("event_count").default(0)
    val postCount = integer("post_count").default(0)
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

val galaxyLightTrigger = CounterTrigger(GalaxyTable, GalaxyLightTable, GalaxyLightTable.galaxyId, GalaxyTable.starCount)
val galaxyPostCountTrigger = CounterTrigger(GalaxyTable, PostTable, PostTable.galaxyId, GalaxyTable.postCount)

fun UpdateBuilder<*>.writeGalaxyFull(galaxy: Galaxy, founderId: StarId, slugRecord: SlugRecord, city: City?, imageSet: SavedImageSet?) {
    this[GalaxyTable.id] = galaxy.galaxyId.value
    this[GalaxyTable.founderId] = founderId.value
    this[GalaxyTable.createdAt] = galaxy.createdAt
    writeGalaxyUpdate(galaxy, slugRecord, city, imageSet)
}

fun UpdateBuilder<*>.writeGalaxyUpdate(galaxy: Galaxy, slugRecord: SlugRecord, city: City?, imageSet: SavedImageSet?) {
    this[GalaxyTable.slug] = slugRecord.slug.string
    this[GalaxyTable.pastSlug] = slugRecord.pastSlug?.string
    this[GalaxyTable.cityId] = city?.cityId?.value
    this[GalaxyTable.city] = city?.name
    this[GalaxyTable.name] = galaxy.name
    this[GalaxyTable.tagline] = galaxy.tagline
    this[GalaxyTable.description] = galaxy.description
    this[GalaxyTable.geoPoint] = galaxy.geoPoint.toPGpoint()
    this[GalaxyTable.geoBounds] = galaxy.geoBounds.toList()
    this[GalaxyTable.postPermission] = galaxy.postPermission
    this[GalaxyTable.reviewMode] = galaxy.reviewMode
    this[GalaxyTable.postGuide] = galaxy.postGuide
    this[GalaxyTable.updatedAt] = galaxy.updatedAt
    writeImages(GalaxyTable.imageConfig, imageSet)
}

fun ResultRow.toGalaxy() = Galaxy(
    galaxyId = toRecordId(GalaxyTable.id),
    cityId = this[GalaxyTable.cityId]?.let { CityId(it.value) },
    city = this[GalaxyTable.city],
    slug = this[GalaxyTable.slug].toSlug(),
    name = this[GalaxyTable.name],
    tagline = this[GalaxyTable.tagline],
    description = this[GalaxyTable.description],
    geoPoint = this[GalaxyTable.geoPoint].toGeoPoint(),
    geoBounds = this[GalaxyTable.geoBounds].toGeoBounds(),
    postPermission = this[GalaxyTable.postPermission],
    reviewMode = this[GalaxyTable.reviewMode],
    postGuide = this[GalaxyTable.postGuide],
    imageRef = this[GalaxyTable.imageRef],
    images = this[GalaxyTable.images],
    lightCount = this[GalaxyTable.starCount],
    eventCount = this[GalaxyTable.eventCount],
    locationCount = this[GalaxyTable.postCount],
    updatedAt = this[GalaxyTable.updatedAt],
    createdAt = this[GalaxyTable.createdAt],
)
