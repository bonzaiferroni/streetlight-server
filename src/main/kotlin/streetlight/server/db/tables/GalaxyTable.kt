package streetlight.server.db.tables

import kampfire.api.Slug
import kampfire.model.ImageSize
import klutch.db.CounterTrigger
import klutch.db.image
import klutch.db.jsonbConfig
import klutch.db.model.CallerId
import klutch.db.point
import klutch.db.tables.SlugTable
import klutch.utils.toList
import klutch.utils.toPGpoint
import klutch.utils.transformMarkdown
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.City
import streetlight.model.data.Galaxy
import streetlight.model.data.PageDesign
import streetlight.model.data.PostPermission
import streetlight.model.data.PostType

object GalaxyTable: UuidTable("galaxy"), SlugTable {
    val starId = reference("star_id", StarTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val cityId = reference("city_id", CityTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val city = text("city").nullable()
    override val slug = text("path").uniqueIndex() // td: rename column as slug
    override val pastSlug = text("past_slug").index().nullable()
    val name = text("name")
    val tagline = text("tagline").nullable()
    val description = text("description").transformMarkdown().nullable()
    val geoPoint = point("geo_point")
    val geoBounds = array<Double>("geo_bounds")
    val postPermission = enumeration<PostPermission>("post_permission")
    val reviewCount = integer("review_count").default(0) // td: remove default value
    val postGuide = text("post_guide").transformMarkdown().nullable()
    val image = image("image").nullable()
    val design = jsonb<PageDesign>("design", jsonbConfig).nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    // denormalized columns
    val starCount = integer("star_count").default(0)
    val eventCount = integer("event_count").default(0)
    val locationCount = integer("location_count").default(0)
    val postCount = integer("post_count").default(0)

    val imageConfig = imageConfigOf(
        table = this,
        column = image,
        ImageSize.Large,
        ImageSize.Medium,
        ImageSize.Small,
        ImageSize.Thumb,
    )
}

val galaxyLocationCountTrigger = CounterTrigger(GalaxyTable, PostTable, PostTable.galaxyId, GalaxyTable.locationCount,
    "NEW.post_type = ${PostType.Location.ordinal}")
val galaxyPostCountTrigger = CounterTrigger(GalaxyTable, PostTable, PostTable.galaxyId, GalaxyTable.postCount)
val galaxyStarTrigger = CounterTrigger(GalaxyTable, GalaxyStarTable, GalaxyStarTable.galaxyId, GalaxyTable.starCount)

fun UpdateBuilder<*>.createGalaxy(galaxy: Galaxy, callerId: CallerId, slug: Slug, city: City?) {
    this[GalaxyTable.id] = galaxy.galaxyId.value
    this[GalaxyTable.starId] = callerId.value
    this[GalaxyTable.slug] = slug.value
    this[GalaxyTable.createdAt] = galaxy.createdAt
    updateGalaxy(galaxy, city)
}

fun UpdateBuilder<*>.updateGalaxy(galaxy: Galaxy, city: City?) {
    this[GalaxyTable.cityId] = city?.cityId?.value
    this[GalaxyTable.city] = city?.name
    this[GalaxyTable.name] = galaxy.name
    this[GalaxyTable.tagline] = galaxy.tagline
    this[GalaxyTable.description] = galaxy.description
    this[GalaxyTable.geoPoint] = galaxy.geoPoint.toPGpoint()
    this[GalaxyTable.geoBounds] = galaxy.geoRect.toList()
    this[GalaxyTable.postPermission] = galaxy.postPermission
    this[GalaxyTable.reviewCount] = galaxy.reviewCount
    this[GalaxyTable.postGuide] = galaxy.postGuide
    this[GalaxyTable.updatedAt] = galaxy.updatedAt
    this[GalaxyTable.image] = galaxy.image
    this[GalaxyTable.design] = galaxy.design
}
