package streetlight.server.db.services

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Locality
import streetlight.server.db.tables.CityTable
import streetlight.server.db.tables.CountryTable
import streetlight.server.db.tables.StateTable
import streetlight.server.utils.toProjectId

object LocalityAspect {
    val columns = listOf(
        CityTable.id,
        CityTable.name,
        CityTable.galaxyCount,
        StateTable.name,
        CountryTable.name
    )

    fun query() = CityTable
        .join(StateTable, JoinType.LEFT, CityTable.stateId, StateTable.id)
        .join(CountryTable, JoinType.LEFT, StateTable.countryId, CountryTable.id)
        .select(columns)
}

fun ResultRow.toLocality() = Locality(
    cityId = this[CityTable.id].toProjectId(),
    city = this[CityTable.name],
    state = this[StateTable.name],
    country = this[CountryTable.name],
    galaxyCount = this[CityTable.galaxyCount]
)