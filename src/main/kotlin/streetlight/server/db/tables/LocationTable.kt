package streetlight.server.db.tables

import kampfire.api.toSlug
import kampfire.model.ImageSize
import klutch.db.SyncValueTrigger
import klutch.utils.*
import klutch.db.point
import klutch.db.scaledImages
import klutch.db.tables.SlugRecord
import klutch.db.tables.SlugTable
import klutch.db.url
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.CityId
import streetlight.model.data.ExtraLink
import streetlight.model.data.Location
import streetlight.model.data.ResourceType
import streetlight.model.data.StarId
import streetlight.server.utils.toRecordId

object LocationTable : UuidTable("location"), SlugTable {
    val hostId = reference("host_id", StarTable, ReferenceOption.SET_NULL).nullable()
    val scoutId = reference("scout_id", StarTable, ReferenceOption.SET_NULL).nullable()
    val cityId = reference("city_id", CityTable).nullable()
    val mapId = long("map_Id").nullable().uniqueIndex()
    override val slug = text("slug").uniqueIndex()
    override val pastSlug = text("past_slug").index().nullable()
    val host = text("host").index().nullable()
    val scout = text("scout").index().nullable()
    val name = text("name").nullable()
    val description = text("description").nullable()
    val address = text("address").nullable()
    val city = text("city").nullable()
    val state = text("state").nullable()
    val geoPoint = point("geo_point")
    val mapRank = float("map_rank").nullable()
    val mapCategory = text("map_category").nullable()
    val mapType = text("map_type").nullable()
    val resources = array<Int>("resources")
    val website = text("link").nullable()
    val starCount = integer("star_count").default(0)
    val links = jsonb<List<ExtraLink>>("links", tableJsonDefault).nullable()
    val eventsUrl = text("events_url").nullable()
    val aboutUrl = text("about_url").nullable()
    val menuUrl = text("menu_url").nullable()
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

val locationCityTrigger = SyncValueTrigger(LocationTable.cityId, LocationTable.city, CityTable, CityTable.name)
val locationStateTrigger = SyncValueTrigger(LocationTable.cityId, LocationTable.state, CityTable, CityTable.state)
val locationHostTrigger = SyncValueTrigger(LocationTable.hostId, LocationTable.host, StarTable, StarTable.username)
val locationScoutTrigger = SyncValueTrigger(LocationTable.scoutId, LocationTable.scout, StarTable, StarTable.username)

val LocationQuery get() = LocationTable.join(StarTable, JoinType.LEFT, LocationTable.hostId, StarTable.id)
    .select(LocationTable.columns + StarTable.username)

// Updaters
fun UpdateBuilder<*>.createRecord(location: Location, starId: StarId?, slugRecord: SlugRecord, imageSet: SavedImageSet?) {
    this[LocationTable.id] = location.locationId.value
    this[LocationTable.scoutId] = starId?.value
    this[LocationTable.createdAt] = location.createdAt
    updateRecord(location, slugRecord, imageSet)
}

fun UpdateBuilder<*>.updateRecord(location: Location, slugRecord: SlugRecord, imageSet: SavedImageSet?) {
    this[LocationTable.slug] = slugRecord.slug.string
    this[LocationTable.pastSlug] = slugRecord.pastSlug?.string
    this[LocationTable.mapId] = location.mapId
    this[LocationTable.cityId] = location.cityId?.value
    // this[LocationTable.ownerId] = ownerId?.toUUID() // td: set owner identity with special pipeline
    this[LocationTable.name] = location.name
    this[LocationTable.description] = location.description
    this[LocationTable.address] = location.address
    this[LocationTable.geoPoint] = location.geoPoint.toPGpoint()
    this[LocationTable.mapCategory] = location.mapCategory
    this[LocationTable.mapType] = location.mapType
    this[LocationTable.mapRank] = location.mapRank
    this[LocationTable.resources] = location.resources.map { it.ordinal }
    this[LocationTable.website] = location.website
    this[LocationTable.eventsUrl] = location.eventsUrl
    this[LocationTable.aboutUrl] = location.aboutUrl
    this[LocationTable.menuUrl] = location.menuUrl
    this[LocationTable.updatedAt] = location.updatedAt
    writeImages(LocationTable.imageConfig, imageSet)
}