package streetlight.server.db.dao

import io.github.oshai.kotlinlogging.KotlinLogging
import kampfire.api.Slug
import kampfire.model.Distance
import kampfire.model.GeoRect
import kampfire.model.GeoPoint
import klutch.db.DbService
import klutch.db.inRect
import klutch.db.isNearEq
import klutch.db.mapFirstOrNull
import klutch.db.model.CallerId
import klutch.db.readFirstOrNull
import klutch.db.withinRadius
import klutch.utils.eq
import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.andIfNotNull
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.wrapAsExpression
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Clock
import streetlight.model.data.Location
import streetlight.model.data.LocationEdit
import kampfire.model.normalize
import kampfire.model.Url
import kampfire.model.toUrl
import streetlight.model.data.ParseConfig
import streetlight.model.data.LocationId
import streetlight.model.data.LocationLead
import streetlight.model.data.ParseMode
import streetlight.model.data.MapId
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
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.CityId
import streetlight.model.data.EntityCursor
import streetlight.model.data.LocationConfig
import streetlight.server.db.tables.afterCursor
import streetlight.server.db.tables.joinWith
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toLocation
import streetlight.server.db.tables.createLocation
import streetlight.server.db.tables.locationConfigContentQuery
import streetlight.server.db.tables.locationLayoutQuery
import streetlight.server.db.tables.locationQuery
import streetlight.server.db.tables.toLocationConfigContent
import streetlight.server.db.tables.toLocationEventFeedOrNull
import streetlight.server.db.tables.LocationAspect
import streetlight.server.db.tables.toLocationDesign
import streetlight.server.db.tables.updateLocation
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
        cityId: CityId?,
        callerId: CallerId?,
        edit: LocationEdit,
        isAdmin: Boolean = false,
    ) = dbQuery {
        val slugBase = edit.getSlugBase(locationId)
        val slugRecord = LocationTable.getSlugRecord(locationId, slugBase)
        val location = edit.toLocation(cityId, locationId)
        val isOwnerOrNull = callerId?.let { LocationTable.hostId.isNull() or LocationTable.hostId.eq(it.value) }
            ?: LocationTable.hostId.isNull()
        val mayEdit = if (isAdmin) null else isOwnerOrNull
        LocationTable.updateReturning(where = { LocationTable.id.eq(locationId) andIfNotNull mayEdit }) {
            it.updateLocation(location, callerId.takeIf { edit.isHost == true }, slugRecord)
        }.singleOrNull()?.toLocation()
    }

    /** Updates the config of a location, only one hosted by [hostId] when given. */
    suspend fun update(config: LocationConfig, hostId: CallerId?) = dbQuery {
        val hostMatch = hostId?.let { LocationTable.hostId.eq(it) }
        LocationTable.update({ LocationTable.id.eq(config.locationId) andIfNotNull hostMatch }) {
            it[LocationTable.parseMode] = config.parseMode
            it[LocationTable.design] = config.design
        }
    }

    suspend fun updateCheckedFeedAt(locationId: LocationId, checkedAt: Instant = Clock.System.now()) = dbQuery {
        LocationTable.update({ LocationTable.id.eq(locationId) }) {
            it[LocationTable.checkedFeedAt] = checkedAt
        }
    }

    suspend fun updateCheckedAt(locationId: LocationId, checkedAt: Instant = Clock.System.now()) = dbQuery {
        LocationTable.update({ LocationTable.id.eq(locationId) }) {
            it[LocationTable.checkedAt] = checkedAt
        }
    }

    /** Counts the upcoming events of each location, updating only the counts that changed. */
    suspend fun updateEventCounts(now: Instant) = dbQuery {
        val upcoming = EventTable.select(EventTable.id.count())
            .where { EventTable.locationId.eq(LocationTable.id) and EventTable.startsAt.greater(now) }
        LocationTable.update({ LocationTable.eventCount.neq(wrapAsExpression(upcoming)) }) {
            it[LocationTable.eventCount] = upcoming
        }
    }

    /** Every location, with its point and the start of its next event after [now]. */
    suspend fun readPriorityInputs(now: Instant) = dbQuery {
        val nextEventAt = EventTable.startsAt.min()
        LocationTable.joinWith(EventTable) { EventTable.startsAt.greater(now) }
            .select(LocationTable.id, LocationTable.geoPoint, nextEventAt)
            .groupBy(LocationTable.id)
            .map {
                LocationPriorityInput(
                    locationId = LocationId(it[LocationTable.id].value),
                    geoPoint = it[LocationTable.geoPoint].toGeoPoint(),
                    nextEventAt = it[nextEventAt],
                )
            }
    }

    suspend fun updateMapPriority(locationId: LocationId, mapPriority: Double) = dbQuery {
        LocationTable.update({ LocationTable.id.eq(locationId) }) {
            it[LocationTable.mapPriority] = mapPriority
        }
    }

    suspend fun create(
        cityId: CityId?,
        callerId: CallerId?,
        edit: LocationEdit,
    ) = dbQuery {
        val locationId = LocationId.random()
        val slugBase = edit.getSlugBase(locationId)
        val slug = LocationTable.nextSlugOf(slugBase)
        val location = edit.toLocation(cityId, locationId)
        LocationTable.insertReturning {
            it.createLocation(location, callerId, callerId.takeIf { edit.isHost == true }, SlugRecord(slug))
        }
            .singleOrNull()?.toLocation()
    }

    suspend fun searchLocations(query: String, city: String?, state: String?, limit: Int = 10) = dbQuery {
        val query = query.lowercase()
        val queryMatch = LocationTable.name.lowerCase().like("%$query%") or
                LocationTable.address.lowerCase().like("$query%") // td: proper address search
        val cityMatch = city?.let {
            LocationTable.city.lowerCase().like("${city.lowercase()}%")
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

    /** The locations whose name holds [name] or whose address holds [address], ignoring case. */
    suspend fun matchLocations(name: String?, address: String?, city: String?, limit: Int = 10) = dbQuery {
        val nameMatch = name?.let { LocationTable.name.lowerCase().like("%${it.lowercase()}%") }
        val addressMatch = address?.let { LocationTable.address.lowerCase().like("%${it.lowercase()}%") }
        val condition = listOfNotNull<Op<Boolean>>(nameMatch, addressMatch).reduceOrNull { a, b -> a or b }
            ?: return@dbQuery emptyList()
        val cityMatch = city?.let { LocationTable.city.lowerCase().like("%${it.lowercase()}%") }
        LocationTable.selectAll().where(cityMatch?.let { condition and it } ?: condition)
            .limit(limit)
            .map { it.toLocation() }
    }

    suspend fun readLocation(locationId: LocationId, callerId: CallerId?) = dbQuery {
        locationQuery(callerId).where { LocationTable.id.eq(locationId) }.mapFirstOrNull { it.toLocation() }
    }

    suspend fun readLocation(slug: Slug, callerId: CallerId?) = dbQuery {
        locationQuery(callerId).where { LocationTable.slug.eq(slug) }.mapFirstOrNull { it.toLocation() }
    }

    /** The locations with the name or the address, ignoring case. */
    suspend fun readLocationsAt(name: String?, address: String?): List<Location> = dbQuery {
        val condition = when {
            name != null && address != null ->
                LocationTable.name.lowerCase().eq(name.lowercase()) or LocationTable.address.lowerCase().eq(address.lowercase())
            name != null -> LocationTable.name.lowerCase().eq(name.lowercase())
            address != null -> LocationTable.address.lowerCase().eq(address.lowercase())
            else -> return@dbQuery emptyList()
        }
        LocationTable.selectAll().where(condition).map { it.toLocation() }
    }

    /** The location of the map place [mapId]. */
    suspend fun readLocationByMapId(mapId: MapId) = dbQuery {
        LocationTable.readFirstOrNull { it.mapId.eq(mapId) }?.toLocation()
    }

    suspend fun readLocationAt(point: GeoPoint) = dbQuery {
        LocationTable.readFirstOrNull { it.geoPoint.isNearEq(point) }?.toLocation()
    }

    /** The page after [cursor] of the named locations in [cityId], newest first, as seen by [callerId]. */
    suspend fun readCityLocations(cityId: CityId, callerId: CallerId?, cursor: EntityCursor.Time) = dbQuery {
        val after = cursor.recordAt?.let { recordAt ->
            cursor.recordId?.let { afterCursor(LocationTable.createdAt, recordAt, LocationTable.id, it, cursor.direction) }
        }
        locationQuery(callerId)
            .where { LocationTable.cityId.eq(cityId) and LocationTable.name.isNotNull() andIfNotNull after }
            .orderBy(LocationTable.createdAt to SortOrder.DESC, LocationTable.id to SortOrder.DESC)
            .limit(EntityCursor.DefaultLimit)
            .map { it.toLocation() }
    }

    /** The locations [callerId] hosts, by name. */
    suspend fun readHostedLocations(callerId: CallerId) = dbQuery {
        locationQuery(callerId)
            .where { LocationTable.hostId.eq(callerId) }
            .orderBy(LocationTable.name)
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

    suspend fun readLocationsInBounds(bounds: GeoRect): List<LocationInfo> = dbQuery {
        LocationTable.leftJoin(EventTable).selectAll()
            .where { LocationTable.geoPoint.inRect(bounds) }
            .toList()
            .groupBy { it[LocationTable.id].toRecordId<LocationId>() }.map { (_, rows) ->
                val location = rows.first().toLocation()
                val events = rows.mapNotNull { row -> row.getOrNull(EventTable.id)?.let { row.toEvent() } }
                LocationInfo(location, events)
            }
    }

    /** The events url of a location and when its feed was last read, only one hosted by [hostId] when given. */
    suspend fun readFeedCheck(locationId: LocationId, hostId: CallerId?) = dbQuery {
        val hostMatch = hostId?.let { LocationTable.hostId.eq(it) }
        LocationTable.select(LocationTable.eventsUrl, LocationTable.checkedFeedAt)
            .where { LocationTable.id.eq(locationId) andIfNotNull hostMatch }
            .singleOrNull()
            ?.let { LocationFeedCheck(it[LocationTable.eventsUrl]?.toUrl(), it[LocationTable.checkedFeedAt]) }
    }

    /**
     * Updates how a location's events are read, only one hosted by [hostId] when given, and clears when its feed was
     * last read once the url changes. Returns the count of locations updated.
     */
    suspend fun update(config: ParseConfig, hostId: CallerId?) = dbQuery {
        val hostMatch = hostId?.let { LocationTable.hostId.eq(it) }
        val where = LocationTable.id.eq(config.locationId) andIfNotNull hostMatch
        val row = LocationTable.select(LocationTable.eventsUrl).where(where).singleOrNull() ?: return@dbQuery 0
        val currentUrl = row[LocationTable.eventsUrl]
        LocationTable.update({ where }) {
            it[LocationTable.eventsUrl] = config.eventsUrl?.value
            it[LocationTable.parseMode] = config.parseMode
            if (config.eventsUrl?.value != currentUrl) it[LocationTable.checkedFeedAt] = null
        }
    }

    /** Whether [callerId] hosts the location. */
    suspend fun isHost(locationId: LocationId, callerId: CallerId) = dbQuery {
        LocationTable.select(LocationTable.id)
            .where { LocationTable.id.eq(locationId) and LocationTable.hostId.eq(callerId) }
            .limit(1)
            .any()
    }

    /** The config content of the locations [callerId] hosts, by name. */
    suspend fun readHostedConfigContents(callerId: CallerId) = dbQuery {
        locationConfigContentQuery().where { LocationTable.hostId.eq(callerId) }
            .orderBy(LocationTable.name)
            .map { it.toLocationConfigContent() }
    }

    /** The config content of a location, only one hosted by [hostId] when given. */
    suspend fun readConfigContent(locationId: LocationId, hostId: CallerId?) = dbQuery {
        val hostMatch = hostId?.let { LocationTable.hostId.eq(it) }
        locationConfigContentQuery().where {
            LocationTable.id.eq(locationId) andIfNotNull hostMatch
        }.singleOrNull()?.toLocationConfigContent()
    }

    /**
     * The events pages of the locations not checked within [interval] whose parse mode reads them, oldest first. A
     * location with no parse mode is read only without a host, since a host has yet to give permission.
     */
    suspend fun readCheckableFeeds(interval: Duration, limit: Int) = dbQuery {
        val isReadable = LocationTable.parseMode.inList(listOf(ParseMode.Partial, ParseMode.Full)) or
            (LocationTable.parseMode.isNull() and LocationTable.hostId.isNull())
        LocationTable.select(LocationAspect.columns + LocationTable.parseMode).where {
             LocationTable.eventsUrl.isNotNull() and isReadable and
                 (LocationTable.checkedFeedAt.isNull() or LocationTable.checkedFeedAt.less(Clock.System.now() - interval))
         }.orderBy(LocationTable.createdAt, SortOrder.ASC)
            .limit(limit)
            .mapNotNull { it.toLocationEventFeedOrNull() }
    }

    /** Leads to read the websites of up to [limit] locations without a host whose website was never read, oldest first. */
    suspend fun readCheckableWebsites(limit: Int) = dbQuery {
        LocationTable.select(LocationTable.id, LocationTable.website, LocationTable.createdAt).where {
            LocationTable.website.isNotNull() and LocationTable.checkedAt.isNull() and LocationTable.hostId.isNull()
        }.orderBy(LocationTable.createdAt, SortOrder.ASC)
            .limit(limit)
            .map { row ->
                LocationLead(
                    leadId = null,
                    initialUrl = requireNotNull(row[LocationTable.website]).toUrl().normalize(),
                    content = null,
                    checkedAt = null,
                    createdAt = row[LocationTable.createdAt],
                    locationId = row.toRecordId(LocationTable.id),
                )
            }
    }

    suspend fun readDesign(slug: Slug, callerId: CallerId?) = dbQuery {
        locationLayoutQuery(callerId).where { LocationTable.slug.eq(slug) }.mapFirstOrNull { it.toLocationDesign() }
    }
}

fun LocationEdit.toLocation(cityId: CityId?, locationId: LocationId) = Location(
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
    tags = tags,
    hours = hours,
    url = website,
    starCount = null,
    eventCount = 0,
    isLit = false,
    eventsUrl = eventsUrl,
    extraLinks = extraLinks,
    image = image,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now()
)

/**
 * The base of the location's slug: its name or else its address, with its city when it has one, or its id when it
 * has neither.
 */
fun LocationEdit.getSlugBase(locationId: LocationId) = (name?.takeIf { it.isNotBlank() } ?: address?.takeIf { it.isNotBlank() })
    ?.let { base -> city?.let { "$base-$it" } ?: base }
    ?: locationId.value.toString()

/** The facts a location's map priority is drawn from. */
data class LocationPriorityInput(
    val locationId: LocationId,
    val geoPoint: GeoPoint,
    val nextEventAt: Instant?,
)

/** The events url of a location, and when its feed was last read. */
data class LocationFeedCheck(
    val eventsUrl: Url?,
    val checkedFeedAt: Instant?,
)
