package streetlight.server.db.services

import kabinet.console.globalConsole
import kampfire.model.Distance
import kampfire.model.GeoBounds
import kampfire.model.GeoPoint
import kampfire.model.UserId
import klutch.db.DbService
import klutch.db.inBounds
import klutch.db.isNearEq
import klutch.db.read
import klutch.db.readById
import klutch.db.readFirst
import klutch.db.readFirstOrNull
import klutch.db.withinRadius
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import klutch.utils.eq
import klutch.utils.toStringId
import klutch.utils.toUUID
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.update
import streetlight.model.data.CommunityId
import streetlight.model.data.Location
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationId
import streetlight.model.data.Place
import streetlight.model.data.toLocation
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.AreaLocationTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.toLocation
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.utils.toProjectId

private val console = globalConsole.getHandle(LocationTableDao::class)

class LocationTableDao: DbService() {

    suspend fun readLocation(locationId: LocationId) = dbQuery {
        LocationTable.read { it.id.eq(locationId) }.firstOrNull()?.toLocation()
    }

    suspend fun readLocations(communityId: CommunityId) = dbQuery {
        // untested
        AreaLocationTable.leftJoin(LocationTable).read { AreaLocationTable.areaId.eq(communityId) }.map { it.toLocation() }
    }

    suspend fun createLocation(userId: UserId, place: Place): LocationId? = dbQuery {
        findOrCreatePlace(place, userId)
    }

    suspend fun updateLocation(userId: UserId, location: Location) = dbQuery {
        LocationTable.update(where = { LocationTable.id.eq(location.locationId) and LocationTable.hostId.eq(userId)}) {
            it.writeUpdate(location)
        } == 1
    }

    suspend fun updateLocation(locationId: LocationId, userId: UserId, edit: LocationEdit) = dbQuery {
        val stored = LocationTable.readFirstOrNull {
            it.id.eq(locationId) and (it.hostId.isNull() or it.hostId.eq(userId))
        }?.toLocation() ?: return@dbQuery null
        val location = edit.toLocation(stored.hostId)
        LocationTable.update(where = { LocationTable.id.eq(locationId) }) { it.writeUpdate(location) }
        LocationTable.readById(locationId.value.toUUID()).toLocation()
    }

    suspend fun createLocation(userId: UserId, edit: LocationEdit) = dbQuery {
        val location = edit.toLocation(if (edit.isHost) userId else null)
        val locationId = LocationTable.insertAndGetId { it.writeFull(location) }.value
        LocationTable.readById(locationId).toLocation()
    }

    suspend fun searchLocations(query: String) = dbQuery {
        val query = query.lowercase()
        LocationTable.read { it.name.lowerCase().like("%$query%") or it.description.lowerCase().like("%$query%") }.map { it.toLocation() }
    }

    suspend fun readTop(count: Int) = dbQuery {
        LocationTable
            .selectAll()
            .orderBy(LocationTable.createdAt, SortOrder.DESC)
            .limit(count)
            .map { it.toLocation() }
    }

    suspend fun readNearbyLocations(point: GeoPoint, distance: Distance) = dbQuery {
        LocationTable.selectAll().withinRadius(LocationTable.geoPoint, point, distance)
            .map { it.toLocation() }
    }

    suspend fun readLocationsInBounds(bounds: GeoBounds) = dbQuery {
        LocationTable.read { it.geoPoint.inBounds(bounds) }.map { it.toLocation() }
    }
}

fun Transaction.findOrCreatePlace(place: Place, userId: UserId): LocationId? {
    val name = place.name
    val geoPoint = place.geoPoint
    if (name == null || geoPoint == null) return null
    val location = LocationTable.readFirstOrNull {
        it.name.eq(name) and it.geoPoint.isNearEq(geoPoint)
    }?.toLocation()

    return if (location != null) {
        console.log("Using stored location ${location.name}")
        location.locationId
    } else {
        console.log("creating new location ${place.name} at ${place.address}")
        LocationTable.insertAndGetId {
            it.writeFull(place.toLocation(userId))
        }.toProjectId()
    }
}