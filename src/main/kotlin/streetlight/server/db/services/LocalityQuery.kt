package streetlight.server.db.services

import klutch.utils.toGeoBounds
import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.CityId
import streetlight.model.data.Locality
import streetlight.server.db.tables.CityTable
import streetlight.server.db.tables.CountryTable
import streetlight.server.db.tables.StateTable

object LocalityAspect {
    val columns = listOf(
        CityTable.id,
        CityTable.name,
        CityTable.galaxyCount,
        CityTable.geoRank,
        CityTable.geoPoint,
        CityTable.geoBounds,
        StateTable.name,
        CountryTable.name
    )

    fun query() = CityTable
        .join(StateTable, JoinType.LEFT, CityTable.stateId, StateTable.id)
        .join(CountryTable, JoinType.LEFT, StateTable.countryId, CountryTable.id)
        .select(columns)
}

fun ResultRow.toLocality() = Locality(
    cityId = CityId(this[CityTable.id].value),
    city = this[CityTable.name],
    state = this[StateTable.name],
    country = this[CountryTable.name],
    galaxyCount = this[CityTable.galaxyCount],
    geoRank = this[CityTable.geoRank],
    geoPoint = this[CityTable.geoPoint].toGeoPoint(),
    geoBounds = this[CityTable.geoBounds].toGeoBounds(),
)