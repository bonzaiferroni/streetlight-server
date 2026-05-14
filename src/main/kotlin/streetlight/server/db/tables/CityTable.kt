package streetlight.server.db.tables

import klutch.db.CounterTrigger
import klutch.utils.*
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.City
import streetlight.model.data.StateId
import streetlight.server.db.tables.CityTable.galaxyCount
import streetlight.server.db.tables.StateTable
import streetlight.server.utils.toProjectId
import kotlin.time.Clock

object CityTable : UUIDTable("city") {
    val stateId = reference("state_id", StateTable.id)
    val name = text("name")
    val aliases = array<String>("aliases")
    val galaxyCount = integer("galaxy_count")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

val galaxyCountTrigger = CounterTrigger(
    parentTable = CityTable,
    parentIdColumn = CityTable.id,
    childTable = GalaxyTable,
    childFkColumn = GalaxyTable.cityId,
    counterColumn = CityTable.galaxyCount
)

fun ResultRow.toCity() = City(
    cityId = this[CityTable.id].toProjectId(),
    name = this[CityTable.name],
    galaxyCount = this[CityTable.galaxyCount],
)

fun UpdateBuilder<*>.writeFull(city: City, stateId: StateId) {
    this[CityTable.id] = city.cityId.toUUID()
    this[CityTable.stateId] = stateId.toUUID()
    this[CityTable.createdAt] = Clock.System.now()
    this[CityTable.aliases] = emptyList()
    this[CityTable.galaxyCount] = 0
    writeUpdate(city)
}

fun UpdateBuilder<*>.writeUpdate(city: City) {
    this[CityTable.name] = city.name
    this[CityTable.updatedAt] = Clock.System.now()
}