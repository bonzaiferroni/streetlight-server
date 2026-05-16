package streetlight.server.db.tables

import klutch.db.CounterTrigger
import klutch.db.point
import klutch.utils.toGeoBounds
import klutch.utils.toGeoPoint
import klutch.utils.toList
import klutch.utils.toPGpoint
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.City
import streetlight.model.data.CityId
import kotlin.time.Clock

object CityTable : IntIdTable("city") {
    val stateId = reference("state_id", StateTable.id)
    val name = text("name")
    val aliases = array<String>("aliases").default(emptyList())
    val galaxyCount = integer("galaxy_count").default(0)
    val geoRank = float("geo_rank")
    val geoPoint = point("geo_point")
    val geoBounds = array<Double>("geo_bounds")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(name, stateId)
    }
}

val cityGalaxyCountTrigger = CounterTrigger(CityTable, GalaxyTable, GalaxyTable.cityId, CityTable.galaxyCount)

fun ResultRow.toCity() = City(
    cityId = CityId(this[CityTable.id].value),
    name = this[CityTable.name],
    galaxyCount = this[CityTable.galaxyCount],
    geoRank = this[CityTable.geoRank],
    geoPoint = this[CityTable.geoPoint].toGeoPoint(),
    geoBounds = this[CityTable.geoBounds].toGeoBounds()
)

fun UpdateBuilder<*>.writeUpdate(city: City) {
    this[CityTable.name] = city.name
    this[CityTable.geoRank] = city.geoRank
    this[CityTable.geoPoint] = city.geoPoint.toPGpoint()
    this[CityTable.geoBounds] = city.geoBounds.toList()
    this[CityTable.updatedAt] = Clock.System.now()
}