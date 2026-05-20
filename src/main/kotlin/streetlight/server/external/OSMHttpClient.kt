package streetlight.server.external

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kampfire.model.GeoPoint
import kotlinx.serialization.json.Json
import streetlight.model.external.OSMCity
import streetlight.model.external.OSMLocation
import streetlight.model.external.OSMQuery
import streetlight.model.external.toOSMCity

class OSMHttpClient(
    userAgent: String = "Streetlight/1.0"
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        defaultRequest {
            headers.append(HttpHeaders.Accept, ContentType.Application.Json.toString())
            headers.append(HttpHeaders.UserAgent, userAgent)
        }
    }

    suspend fun search(point: GeoPoint): OSMLocation? =
        client.get("https://nominatim.openstreetmap.org/reverse") {
            url {
                parameters.append("lat", point.lat.toString())
                parameters.append("lon", point.lng.toString())
                parameters.append("format", "jsonv2")
                parameters.append("addressdetails", "1")
            }
        }.body()

    suspend fun search(query: OSMQuery): List<OSMLocation>? =
        client.get("https://nominatim.openstreetmap.org/search") {
            url {
                query.amenity?.let { parameters.append("amenity", it) }
                query.street?.let { parameters.append("street", it) }
                query.city?.let { parameters.append("city", it) }
                query.county?.let { parameters.append("county", it) }
                query.state?.let { parameters.append("state", it) }
                query.country?.let { parameters.append("country", it) }
                query.postalcode?.let { parameters.append("postalcode", it) }
                parameters.append("format", "jsonv2")
                parameters.append("addressdetails", "1")
                parameters.append("limit", query.limit.toString())
            }
        }.body()

    suspend fun searchCity(query: String, country: String): List<OSMCity>? =
        search(OSMQuery(city = query, country = country))?.map { it.toOSMCity() }

    suspend fun readCity(city: String, state: String): OSMCity? =
        search(OSMQuery(city = city, state = state))?.firstOrNull()?.toOSMCity()

    suspend fun search(city: String, state: String): List<OSMLocation>? =
        search(OSMQuery(city = city, state = state))
}
