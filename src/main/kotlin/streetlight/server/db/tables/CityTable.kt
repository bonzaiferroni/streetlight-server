package streetlight.server.db.tables

import kampfire.api.toSlug
import klutch.db.CounterTrigger
import klutch.db.SyncValueTrigger
import klutch.db.point
import klutch.db.tables.SlugTable
import klutch.utils.toGeoBounds
import klutch.utils.toGeoPoint
import klutch.utils.toList
import klutch.utils.toPGpoint
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.City
import streetlight.model.data.CityId
import streetlight.model.data.StateId
import kotlin.time.Clock
import kotlin.uuid.Uuid

object CityTable : UuidTable("city"), SlugTable {
    val stateId = reference("state_id", StateTable.id).index()
    override val slug = text("slug").index()
    val name = text("name")
    val aliases = array<String>("aliases").default(emptyList())
    val galaxyCount = integer("galaxy_count").default(0)
    val locationCount = integer("location_count").default(0)
    val state = text("state")
    val country = text("country")
    val geoPoint = point("geo_point") // index
    val geoBounds = array<Double>("geo_bounds")
    val mapRank = float("map_rank").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(name, stateId)
    }
}

val cityGalaxyTrigger = CounterTrigger(CityTable, GalaxyTable, GalaxyTable.cityId, CityTable.galaxyCount)
val cityLocationCountTrigger = CounterTrigger(CityTable, LocationTable, LocationTable.cityId, CityTable.locationCount)
val cityStateSync = SyncValueTrigger(CityTable.stateId, CityTable.state, StateTable, StateTable.name)
val cityCountrySync = SyncValueTrigger(CityTable.stateId, CityTable.country, StateTable, StateTable.country)

fun ResultRow.toCity() = City(
    cityId = CityId(this[CityTable.id].value),
    slug = this[CityTable.slug].toSlug(),
    name = this[CityTable.name],
    state = this[CityTable.state],
    country = this[CityTable.country],
    galaxyCount = this[CityTable.galaxyCount],
    locationCount = this[CityTable.locationCount],
    mapRank = this[CityTable.mapRank],
    geoPoint = this[CityTable.geoPoint].toGeoPoint(),
    geoRect = this[CityTable.geoBounds].toGeoBounds()
)

fun UpdateBuilder<*>.createCity(city: City, stateId: StateId) {
    this[CityTable.id] = Uuid.random()
    this[CityTable.stateId] = stateId.value
    this[CityTable.slug] = city.slug.value
    this[CityTable.state] = city.state
    this[CityTable.country] = city.country
    this[CityTable.createdAt] = Clock.System.now()
    updateCity(city)
}

fun UpdateBuilder<*>.updateCity(city: City) {
    this[CityTable.name] = city.name
    this[CityTable.mapRank] = city.mapRank
    this[CityTable.geoPoint] = city.geoPoint.toPGpoint()
    this[CityTable.geoBounds] = city.geoRect.toList()
    this[CityTable.updatedAt] = Clock.System.now()
}