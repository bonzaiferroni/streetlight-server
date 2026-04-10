package streetlight.server.routes

import com.google.transit.realtime.GtfsRealtime
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import kabinet.console.globalConsole
import kampfire.model.GeoPoint
import klutch.server.getEndpoint
import klutch.server.readParam
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import streetlight.model.Api
import streetlight.model.data.AreaTransit
import streetlight.model.data.AreaTransitState
import streetlight.model.data.TransitVehicle
import streetlight.server.model.*
import kotlin.time.Duration.Companion.seconds

private val httpClient = HttpClient(CIO)
private val console = globalConsole.getHandle(StreetlightRouting::serveGtfs.name)

fun StreetlightRouting.serveGtfs() {
    CoroutineScope(Dispatchers.IO).launch {
        initGtfs(app)
    }

    var vehiclePositionBytes: ByteArray? = null
    var contentType: ContentType = ContentType.Application.OctetStream
    var lastGtfsAt: Instant = Instant.DISTANT_PAST

    get(Api.Gtfs.VehiclePosition.path) {
        val bytes = if (vehiclePositionBytes != null && Clock.System.now() - lastGtfsAt < 10.seconds) {
            vehiclePositionBytes!!
        } else {
            try {
                val url = "https://open-data.rtd-denver.com/files/gtfs-rt/rtd/VehiclePosition.pb"
                val upstreamResponse: HttpResponse = httpClient.get(url)
                contentType = upstreamResponse.headers[HttpHeaders.ContentType]
                    ?.let { ContentType.parse(it) }
                    ?: ContentType.Application.OctetStream
                upstreamResponse.readRawBytes().also {
                    vehiclePositionBytes = it
                    lastGtfsAt = Clock.System.now()
                }
            } catch (e: Exception) {
                console.logWarning("unable to get gtfs: ${e.message}")
                null
            }
        } ?: return@get

        call.respondBytes(
            bytes = bytes,
            contentType = contentType
        )
    }

    var cachedState: AreaTransitState? = null
    var lastReadAt: Instant = Instant.DISTANT_PAST

    getEndpoint(Api.Gtfs.TransitState) { endpoint ->
        val requestTimestamp = readParam(endpoint.timestamp)
        val lastState = cachedState

        val now = Clock.System.now()
        val state = if (lastState != null && now - lastReadAt < 10.seconds) {
            lastState
        } else {
            try {
                lastReadAt = now
                val url = "https://open-data.rtd-denver.com/files/gtfs-rt/rtd/VehiclePosition.pb"
                val response = httpClient.get(url)
                val bytes = response.readRawBytes()
                val feed = GtfsRealtime.FeedMessage.parseFrom(bytes)
                val timestamp = feed.header.timestamp
                if (lastState != null && timestamp == lastState.timestamp) {
                    lastState
                } else {
                    val vehicles = feed.entityList.map { it.vehicle.toTransitVehicle() }
                    AreaTransitState(
                        timestamp = timestamp,
                        vehicles = vehicles,
                    ).also { cachedState = it }
                }
            } catch (e: Exception) {
                console.logWarning("unable to get gtfs: ${e.message}")
                null
            }
        }

        if (state != null && requestTimestamp < state.timestamp) {
            state
        } else {
            call.respond(HttpStatusCode.NoContent)
            null
        }
    }

    getEndpoint(Api.Gtfs.Routes) {
        val routeIds = setOf("101E", "101D", "103W", "117N", "107R", "101H", "A")
        val routes = app.dao.transitRoute.readRoutes(routeIds)
        val stops = app.dao.transitStop.readRouteStops(routeIds)
        AreaTransit(
            routes = routes,
            stops = stops
        )
    }
}

fun GtfsRealtime.VehiclePosition.toTransitVehicle() = TransitVehicle(
    vehicleId = vehicle.id,
    routeId = trip.routeId,
    geoPoint = position.toGeoPoint(),
    bearing = position.bearing,
    timestamp = timestamp
)

fun GtfsRealtime.Position.toGeoPoint() = GeoPoint(
    lng = longitude.toDouble(),
    lat = latitude.toDouble(),
)