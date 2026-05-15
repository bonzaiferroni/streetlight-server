package streetlight.server.db.services

import klutch.db.DbService
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.City
import streetlight.model.data.CityId
import streetlight.model.data.Locality
import streetlight.model.data.State
import streetlight.server.db.tables.CityTable
import streetlight.server.db.tables.CountryTable
import streetlight.server.db.tables.StateTable
import streetlight.server.db.tables.toCity
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import kotlin.time.Clock

class LocalityTableDao : DbService() {

    suspend fun readCity(cityId: CityId) = dbQuery {
        CityTable.selectAll().where { CityTable.id eq cityId.value }.firstOrNull()?.toCity()
    }

    suspend fun readCities() = dbQuery {
        CityTable.selectAll().map { it.toCity() }
    }

    suspend fun update(city: City) = dbQuery {
        CityTable.update({ CityTable.id eq city.cityId.value }) {
            it.writeUpdate(city)
        } > 0
    }

    suspend fun delete(cityId: CityId) = dbQuery {
        CityTable.deleteWhere { CityTable.id eq cityId.value } > 0
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

    suspend fun searchLocalities(query: String, limit: Int = 10) = dbQuery {
        LocalityAspect.query()
            .where { CityTable.name.like("${query.lowercase()}%")}
            .limit(limit)
            .map { it.toLocality() }
    }

    suspend fun createCities(localities: List<Locality>) = dbQuery {
        val now = Clock.System.now()

        // 1. Countries
        val uniqueCountries = localities.map { it.country }.distinct()
        CountryTable.batchInsert(uniqueCountries, ignore = true) {
            this[CountryTable.name] = it
            this[CountryTable.createdAt] = now
            this[CountryTable.updatedAt] = now
        }
        val countryIds: Map<String, Int> = CountryTable
            .selectAll()
            .where { CountryTable.name.inList(uniqueCountries) }
            .associate { it[CountryTable.name] to it[CountryTable.id].value }

        // 2. States
        val uniqueStates = localities.map { it.state to it.country }.distinct()
        StateTable.batchInsert(uniqueStates, ignore = true) { (state, country) ->
            this[StateTable.countryId] = countryIds.getValue(country)
            this[StateTable.name] = state
            this[StateTable.createdAt] = now
            this[StateTable.updatedAt] = now
        }
        val stateIds: Map<Pair<String, String>, Int> = StateTable
            .innerJoin(CountryTable)
            .select(StateTable.id, StateTable.name, CountryTable.name)
            .where { CountryTable.name.inList(uniqueCountries) }
            .associate { (it[StateTable.name] to it[CountryTable.name]) to it[StateTable.id].value }

        // 3. Cities
        CityTable.batchInsert(localities, ignore = true) {
            this[CityTable.name] = it.city
            this[CityTable.stateId] = stateIds.getValue(it.state to it.country)
            this[CityTable.createdAt] = now
            this[CityTable.updatedAt] = now
        }

        val cityNames = localities.map { it.city }.distinct()
        LocalityAspect.query()
            .where { CityTable.name.inList(cityNames) and CityTable.stateId.inList(stateIds.values) }
            .map { it.toLocality() }
    }
}
