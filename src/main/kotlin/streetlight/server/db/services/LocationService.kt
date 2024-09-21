package streetlight.server.db.services

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import streetlight.model.core.Location
import streetlight.server.db.DataService
import streetlight.server.db.findUserIdOrThrow
import streetlight.server.db.tables.LocationEntity
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.fromData
import streetlight.server.db.tables.toData

class LocationService : DataService<Location, LocationEntity>(
    LocationEntity,
    LocationEntity::fromData,
    LocationEntity::toData,
) {
    override fun getSearchOp(search: String): Op<Boolean> =
        Op.build { LocationTable.name.lowerCase() like "${search.lowercase()}%" }

    suspend fun getUserLocation(username: String, locationId: Int): Location = dbQuery {
        val userId = findUserIdOrThrow(username)
        LocationEntity.find { (LocationTable.id eq locationId) and (LocationTable.userId eq userId) }
            .firstOrNull()?.toData() ?: throw IllegalArgumentException("Location not found")
    }

    suspend fun deleteUserLocation(username: String, locationId: Int) = dbQuery {
        val userId = findUserIdOrThrow(username)
        LocationEntity.find { (LocationTable.id eq locationId) and (LocationTable.userId eq userId) }
            .firstOrNull()?.delete() ?: throw IllegalArgumentException("Location not found")
    }

    suspend fun getUserLocations(username: String): List<Location> = dbQuery {
        val userId = findUserIdOrThrow(username)
        LocationEntity.find { LocationTable.userId eq userId }.map { it.toData() }
    }

    suspend fun createUserLocation(username: String, data: Location): Location = dbQuery {
        val userId = findUserIdOrThrow(username)
        LocationEntity.new { fromData(data.copy(userId = userId.value)) }.toData()
    }

    suspend fun updateUserLocation(username: String, data: Location): Location = dbQuery {
        val userId = findUserIdOrThrow(username)
        val location = LocationEntity.findSingleByAndUpdate(
            (LocationTable.id eq data.id) and (LocationTable.userId eq userId)
        ) {
            it.fromData(data.copy(userId = userId.value))
        } ?: throw IllegalArgumentException("Location not found")
        location.toData()
    }
}

