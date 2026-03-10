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
import klutch.db.readFirstOrNull
import klutch.db.withinRadius
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import klutch.utils.eq
import klutch.utils.toUUID
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.update
import streetlight.model.data.GalaxyId
import streetlight.model.data.Location
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationId
import streetlight.model.data.LocationInfo
import streetlight.model.data.Place
import streetlight.model.data.toLocation
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.GalaxyLocationTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toLocation
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.utils.toProjectId

private val console = globalConsole.getHandle(LocationTableDao::class)

class LocationTableDao : DbService() {

    suspend fun readLocation(locationId: LocationId) = dbQuery {
        LocationTable.read { it.id.eq(locationId) }.firstOrNull()?.toLocation()
    }

    suspend fun readLocations(galaxyId: GalaxyId) = dbQuery {
        // untested
        GalaxyLocationTable.leftJoin(LocationTable).read { GalaxyLocationTable.galaxyId.eq(galaxyId) }
            .map { it.toLocation() }
    }

    suspend fun readLocationAt(name: String?, address: String?) = dbQuery {
        if (name != null && address != null) {
            LocationTable.readFirstOrNull { it.name.lowerCase().eq(name.lowercase()) or it.address.lowerCase().eq(address.lowercase()) }
                ?.toLocation()
        } else if (name != null) {
            LocationTable.readFirstOrNull { it.name.lowerCase().eq(name.lowercase()) }?.toLocation()
        } else if (address != null) {
            LocationTable.readFirstOrNull { it.address.lowerCase().eq(address.lowercase()) }?.toLocation()
        } else null
    }

    suspend fun readLocationAt(point: GeoPoint) = dbQuery {
        LocationTable.readFirstOrNull { it.geoPoint.isNearEq(point) }?.toLocation()
    }

    suspend fun createLocation(userId: UserId, place: Place, isHost: Boolean): LocationId? = dbQuery {
        findOrCreatePlace(place, userId, isHost)
    }

    suspend fun updateLocation(userId: UserId, location: Location) = dbQuery {
        LocationTable.update(where = { LocationTable.id.eq(location.locationId) and LocationTable.hostId.eq(userId) }) {
            it.writeUpdate(location)
        } == 1
    }

    suspend fun updateLocation(locationId: LocationId, userId: UserId, edit: LocationEdit) = dbQuery {
        val location = edit.toLocation()
        LocationTable.update(where = {
            LocationTable.id.eq(locationId) and (LocationTable.hostId.isNull() or LocationTable.hostId.eq(userId))
        }) { it.writeUpdate(location) }
        LocationTable.readById(locationId.value.toUUID()).toLocation()
    }

    suspend fun createLocation(userId: UserId, edit: LocationEdit) = dbQuery {
        val location = edit.toLocation()
        val locationId =
            LocationTable.insertAndGetId { it.writeFull(location, if (edit.isHost) userId else null) }.value
        LocationTable.readById(locationId).toLocation()
    }

    suspend fun searchLocations(query: String) = dbQuery {
        val query = query.lowercase()
        LocationTable.read {
            (it.name.lowerCase().like("%$query%") or it.description.lowerCase().like("%$query%")) or it.address.lowerCase().like("%$query%")
        }
            .map { it.toLocation() }
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

    suspend fun readLocationsInBounds(bounds: GeoBounds): List<LocationInfo> = dbQuery {
        LocationTable.leftJoin(EventTable).selectAll().where { LocationTable.geoPoint.inBounds(bounds) }
            .toList()
            .groupBy { it[LocationTable.id].toProjectId<LocationId>() }.map { (_, rows) ->
                val location = rows.first().toLocation()
                val events = rows.mapNotNull { row -> row.getOrNull(EventTable.id)?.let { row.toEvent() } }
                LocationInfo(location, events)
            }
    }
}

fun Transaction.findOrCreatePlace(place: Place, userId: UserId, isHost: Boolean): LocationId? {
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
            it.writeFull(place.toLocation(), if (isHost) userId else null)
        }.toProjectId()
    }
}