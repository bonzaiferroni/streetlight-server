package streetlight.server.db.tables

import kampfire.model.CallerId
import kampfire.model.ImageSize
import klutch.db.SyncValueTrigger
import klutch.db.image
import klutch.db.jsonColumnConfig
import klutch.utils.*
import klutch.db.point
import klutch.db.tables.SlugRecord
import klutch.db.tables.SlugTable
import koala.Image
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.ExtraLink
import streetlight.model.data.HoursSchedule
import streetlight.model.data.Location
import streetlight.model.data.StarId

object LocationTable: UuidTable("location"), SlugTable {
    val hostId = reference("host_id", StarTable, ReferenceOption.SET_NULL).nullable()
    val scoutId = reference("scout_id", StarTable, ReferenceOption.SET_NULL).nullable()
    val cityId = reference("city_id", CityTable).nullable()
    val mapId = long("map_Id").nullable().uniqueIndex()
    val timezoneId = text("timezone_id").default("America/Denver") // td: remove default
    override val slug = text("slug").uniqueIndex()
    override val pastSlug = text("past_slug").index().nullable()
    val host = text("host").index().nullable()
    val scout = text("scout").index().nullable()
    val name = text("name").nullable()
    val description = text("description").transformMarkdown().nullable()
    val address = text("address").nullable()
    val city = text("city").nullable()
    val state = text("state").nullable()
    val geoPoint = point("geo_point")
    val mapRank = float("map_rank").nullable()
    val mapCategory = text("map_category").nullable()
    val mapType = text("map_type").nullable()
    val resources = array<Int>("resources")
    val hours = jsonb<HoursSchedule>("hours", jsonColumnConfig).nullable()
    val website = text("link").nullable()
    val starCount = integer("star_count").default(0)
    val links = jsonb<List<ExtraLink>>("links", jsonColumnConfig).nullable()
    val eventsUrl = text("events_url").nullable()
    val image = image("image").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    val imageConfig = imageConfigOf(
        table = this,
        column = image,
        ImageSize.Large,
        ImageSize.Medium,
        ImageSize.Small,
        ImageSize.Thumb,
    )
}

val locationCitySync = SyncValueTrigger(LocationTable.cityId, LocationTable.city, CityTable, CityTable.name)
val locationStateSync = SyncValueTrigger(LocationTable.cityId, LocationTable.state, CityTable, CityTable.state)
val locationHostSync = SyncValueTrigger(LocationTable.hostId, LocationTable.host, StarTable, StarTable.username)
val locationScoutSync = SyncValueTrigger(LocationTable.scoutId, LocationTable.scout, StarTable, StarTable.username)

// Updaters
fun UpdateBuilder<*>.createRecord(location: Location, starId: CallerId?, slugRecord: SlugRecord) {
    this[LocationTable.id] = location.locationId.value
    this[LocationTable.scoutId] = starId?.value
    this[LocationTable.createdAt] = location.createdAt
    updateRecord(location, slugRecord)
}

fun UpdateBuilder<*>.updateRecord(location: Location, slugRecord: SlugRecord) {
    this[LocationTable.slug] = slugRecord.slug.value
    this[LocationTable.pastSlug] = slugRecord.pastSlug?.value
    this[LocationTable.mapId] = location.mapId
    this[LocationTable.timezoneId] = location.timezoneId
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
    this[LocationTable.hours] = location.hours
    this[LocationTable.website] = location.website
    this[LocationTable.eventsUrl] = location.eventsUrl
    this[LocationTable.links] = location.extraLinks
    this[LocationTable.updatedAt] = location.updatedAt
    this[LocationTable.image] = location.image
}