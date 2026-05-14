package streetlight.server.db.services

import klutch.db.DbService
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.City
import streetlight.model.data.CityId
import streetlight.server.db.tables.CityTable
import streetlight.server.db.tables.toCity
import streetlight.server.db.tables.writeUpdate

class LocalityTableDao : DbService() {

    suspend fun readCity(cityId: CityId) = dbQuery {
        CityTable.selectAll().where { CityTable.id eq cityId.toUUID() }.firstOrNull()?.toCity()
    }

    suspend fun readCities() = dbQuery {
        CityTable.selectAll().map { it.toCity() }
    }

    suspend fun update(city: City) = dbQuery {
        CityTable.update({ CityTable.id eq city.cityId.toUUID() }) {
            it.writeUpdate(city)
        } > 0
    }

    suspend fun delete(cityId: CityId) = dbQuery {
        CityTable.deleteWhere { CityTable.id eq cityId.toUUID() } > 0
    }

//    suspend fun readOrCreateCity(name: String) = dbQuery {
//        CityTable.selectAll()
//            .where { CityTable.name.eqIgnoreCase(name) or CityTable.aliases.anyEqIgnoreCase(name) }
//            .firstOrNull()?.toCity() ?: City(
//            cityId = CityId.random(),
//            name = name,
//            galaxyCount = 0,
//        ).also { city ->
//            CityTable.insert {
//                it.writeFull(city)
//            }
//        }
//    }

    suspend fun readTopCities(limit: Int = 10) = dbQuery {
        LocalityAspect.query()
            .orderBy(CityTable.galaxyCount, SortOrder.DESC)
            .limit(limit)
            .map { it.toLocality() }
    }

    suspend fun searchLocalities(query: String) = dbQuery {
        LocalityAspect.query()
            .where { CityTable.name.like("${query.lowercase()}%")}
            .map { it.toLocality() }
    }
}
