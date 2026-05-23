package streetlight.server.db.services

import kabinet.console.globalConsole
import kampfire.api.Slug
import kampfire.model.Distance
import kampfire.model.GeoBounds
import kampfire.model.GeoPoint
import klutch.db.DbService
import klutch.db.inBounds
import klutch.db.isNearEq
import klutch.db.readById
import klutch.db.readFirstOrNull
import klutch.db.withinRadius
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.andIfNotNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import streetlight.model.data.Location
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationId
import streetlight.model.data.LocationInfo
import streetlight.model.data.StarId
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationQuery
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.SavedImageSet
import streetlight.server.db.tables.readSlug
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toLocation
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.utils.toProjectId

private val console = globalConsole.getHandle(LocationTableDao::class)

class LocationTableDao : DbService() {

    suspend fun readLocation(locationId: LocationId) = dbQuery {
        LocationQuery.where { LocationTable.id.eq(locationId) }.firstOrNull()?.toLocation()
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

//    suspend fun createLocation(userId: UserId, place: Place, isHost: Boolean): LocationId? = dbQuery {
//        findOrCreatePlace(place, userId, isHost)
//    }

//    suspend fun updateLocation(userId: UserId, location: Location) = dbQuery {
//        LocationTable.update(where = { LocationTable.id.eq(location.locationId) and LocationTable.creatorId.eq(userId) }) {
//            it.writeUpdate(location)
//        } == 1
//    }

    suspend fun updateLocation(
        locationId: LocationId,
        starId: StarId?,
        edit: LocationEdit,
        imageSet: SavedImageSet?
    ) = dbQuery {
        val slugBase = edit.getSlugBase(locationId)
        val slug = LocationTable.nextSlugOf(slugBase)
        val location = edit.toLocation(locationId, slug)
        val isOwnerOrNull = LocationTable.ownerId.isNull() or LocationTable.ownerId.eq(starId?.value)
        LocationTable.update(where = { LocationTable.id.eq(locationId) and isOwnerOrNull }) {
            it.writeUpdate(location, imageSet)
        }
        locationId
    }

    suspend fun createLocation(starId: StarId?, edit: LocationEdit, imageSet: SavedImageSet?) = dbQuery {
        val locationId = LocationId.random()
        val slugBase = edit.getSlugBase(locationId)
        val slug = LocationTable.nextSlugOf(slugBase)
        val location = edit.toLocation(locationId, slug)
        LocationTable.insert { it.writeFull(location, starId, imageSet) }
        locationId
    }

    suspend fun searchLocations(query: String, city: String?, state: String?, limit: Int = 10) = dbQuery {
        val query = query.lowercase()
        val queryMatch = LocationTable.name.lowerCase().like("%$query%") or
                LocationTable.address.lowerCase().like("$query%") // td: proper address search
        val cityMatch = city?.let {
            LocationTable.city.lowerCase().eq(city.lowercase())
        }
        val stateMatch = state?.let {
            LocationTable.state.lowerCase().eq(state.lowercase())
        }
        LocationTable.selectAll().where {
            queryMatch.andIfNotNull(cityMatch).andIfNotNull(stateMatch)
        }
            .limit(limit)
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



fun LocationEdit.toLocation(locationId: LocationId, slug: Slug) = Location(
    locationId = locationId,
    cityId = cityId,
    mapId = mapId,
    slug = slug,
    name = name,
    geoPoint = geoPoint ?: error("no location geoPoint"),
    mapRank = mapRank,
    mapCategory = mapCategory,
    mapType = mapType,
    description = description,
    address = address,
    city = city,
    state = state,
    resources = resources ?: emptySet(),
    website = website,
    lightCount = null,
    eventsUrl = eventsUrl,
    aboutUrl = aboutUrl,
    menuUrl = menuUrl,
    imageRef = imageRef,
    images = null,
    extraLinks = null,
    username = null,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now()
)

fun LocationEdit.getSlugBase(locationId: LocationId) = when {
    name != null && city != null -> "$name-$city"
    else -> locationId.value.toString()
}