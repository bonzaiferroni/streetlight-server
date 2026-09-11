package streetlight.server.db.services

import io.github.oshai.kotlinlogging.KotlinLogging
import kampfire.api.Slug
import kampfire.model.Distance
import kampfire.model.GeoBounds
import kampfire.model.GeoPoint
import klutch.db.DbService
import klutch.db.inBounds
import klutch.db.isNearEq
import klutch.db.mapFirstOrNull
import klutch.db.model.CallerId
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
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Clock
import streetlight.model.data.Location
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationId
import streetlight.model.data.LocationInfo
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationTable
import klutch.db.tables.SlugRecord
import klutch.db.tables.getSlugRecord
import klutch.db.tables.nextSlugOf
import klutch.utils.logger
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.CityId
import streetlight.model.data.LocationConfig
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toLocation
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.locationConfigContentQuery
import streetlight.server.db.tables.locationLayoutQuery
import streetlight.server.db.tables.locationQuery
import streetlight.server.db.tables.toLocationConfigContent
import streetlight.server.db.tables.toLocationDesign
import streetlight.server.db.tables.updateRecord
import streetlight.server.utils.toRecordId
import kotlin.time.Duration
import kotlin.time.Instant

private val log = KotlinLogging.logger(LocationTableDao::class)

class LocationTableDao : DbService() {

//    suspend fun createLocation(userId: UserId, place: Place, isHost: Boolean): LocationId? = dbQuery {
//        findOrCreatePlace(place, userId, isHost)
//    }

//    suspend fun updateLocation(userId: UserId, location: Location) = dbQuery {
//        LocationTable.update(where = { LocationTable.id.eq(location.locationId) and LocationTable.creatorId.eq(userId) }) {
//            it.writeUpdate(location)
//        } == 1
//    }

    suspend fun update(
        locationId: LocationId,
        cityId: CityId,
        callerId: CallerId,
        edit: LocationEdit,
    ) = dbQuery {
        val slugBase = edit.getSlugBase(locationId)
        val slugRecord = LocationTable.getSlugRecord(locationId, slugBase)
        val location = edit.toLocation(cityId, locationId)
        val isOwnerOrNull = LocationTable.hostId.isNull() or LocationTable.hostId.eq(callerId.value)
        LocationTable.updateReturning(where = { LocationTable.id.eq(locationId) and isOwnerOrNull }) {
            it.updateRecord(location, slugRecord)
        }.singleOrNull()?.toLocation()
    }

    suspend fun update(config: LocationConfig) = dbQuery {
        LocationTable.update({ LocationTable.id.eq(config.locationId) }) {
            it[LocationTable.parseMode] = config.parseMode
            it[LocationTable.design] = config.design
        }
    }

    suspend fun updateCheckedAt(locationId: LocationId, checkedAt: Instant = Clock.System.now()) = dbQuery {
        LocationTable.update({ LocationTable.id.eq(locationId) }) {
            it[LocationTable.checkedAt] = checkedAt
        }
    }

    suspend fun create(
        cityId: CityId,
        callerId: CallerId,
        edit: LocationEdit,
    ) = dbQuery {
        val locationId = LocationId.random()
        val slugBase = edit.getSlugBase(locationId)
        val slug = LocationTable.nextSlugOf(slugBase)
        val location = edit.toLocation(cityId, locationId)
        LocationTable.insert { it.createRecord(location, callerId, SlugRecord(slug)) }
            .resultedValues?.singleOrNull()?.toLocation()
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

    suspend fun readLocation(locationId: LocationId, callerId: CallerId?) = dbQuery {
        locationQuery(callerId).where { LocationTable.id.eq(locationId) }.mapFirstOrNull { it.toLocation() }
    }

    suspend fun readLocation(slug: Slug, callerId: CallerId?) = dbQuery {
        locationQuery(callerId).where { LocationTable.slug.eq(slug) }.mapFirstOrNull { it.toLocation() }
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
        LocationTable.leftJoin(EventTable).selectAll()
            .where { LocationTable.geoPoint.inBounds(bounds) }
            .toList()
            .groupBy { it[LocationTable.id].toRecordId<LocationId>() }.map { (_, rows) ->
                val location = rows.first().toLocation()
                val events = rows.mapNotNull { row -> row.getOrNull(EventTable.id)?.let { row.toEvent() } }
                LocationInfo(location, events)
            }
    }

    suspend fun readConfigContent(locationId: LocationId) = dbQuery {
        locationConfigContentQuery().where {
            LocationTable.id.eq(locationId)
        }.singleOrNull()?.toLocationConfigContent()
    }

    suspend fun readCheckable(interval: Duration) = dbQuery {
        locationConfigContentQuery().where {
             LocationTable.eventsUrl.isNotNull() and (LocationTable.checkedAt.isNull() or LocationTable.checkedAt.less(Clock.System.now() - interval))
         }.map { it.toLocationConfigContent() }
    }

    suspend fun readDesign(slug: Slug, callerId: CallerId?) = dbQuery {
        locationLayoutQuery(callerId).where { LocationTable.slug.eq(slug) }.mapFirstOrNull { it.toLocationDesign() }
    }
}

fun LocationEdit.toLocation(cityId: CityId, locationId: LocationId) = Location(
    locationId = locationId,
    cityId = cityId,
    mapId = mapId,
    timezoneId = timezoneId ?: error("no timezone provided"),
    slug = Slug.Empty,
    host = null, // set with trigger
    scout = null, // set with trigger
    name = name,
    geoPoint = geoPoint ?: error("no location geoPoint"),
    mapRank = mapRank, // td: more credible source for mapRank
    mapCategory = mapCategory,
    mapType = mapType,
    description = description,
    address = address,
    city = city,
    state = state,
    resources = resources ?: emptySet(),
    hours = hours,
    website = website,
    starCount = null,
    isLit = false,
    eventsUrl = eventsUrl,
    extraLinks = extraLinks,
    image = image,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now()
)

fun LocationEdit.getSlugBase(locationId: LocationId) = when {
    name != null && city != null -> "$name-$city"
    else -> locationId.value.toString()
}