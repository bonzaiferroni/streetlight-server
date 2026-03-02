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
import streetlight.model.external.OSMPlace
import streetlight.model.external.OSMQuery

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

    suspend fun readPlace(point: GeoPoint): OSMPlace? =
        client.get("https://nominatim.openstreetmap.org/reverse") {
            url {
                parameters.append("lat", point.lat.toString())
                parameters.append("lon", point.lng.toString())
                parameters.append("format", "json")
            }
        }.body()

    suspend fun readPlace(query: OSMQuery): List<OSMPlace>? =
        client.get("https://nominatim.openstreetmap.org/search") {
            url {
                query.amenity?.let { parameters.append("amenity", it) }
                query.street?.let { parameters.append("street", it) }
                query.city?.let { parameters.append("city", it) }
                query.county?.let { parameters.append("county", it) }
                query.state?.let { parameters.append("state", it) }
                query.country?.let { parameters.append("country", it) }
                query.postalcode?.let { parameters.append("postalcode", it) }
                parameters.append("format", query.format)
                parameters.append("addressdetails", query.addressdetails.toString())
                parameters.append("limit", query.limit.toString())
            }
        }.body()
}