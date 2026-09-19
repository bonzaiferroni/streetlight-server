package streetlight.server.model

import kampfire.model.GeoPoint
import streetlight.model.external.OSMCity
import streetlight.model.external.OSMLocation
import streetlight.model.external.OSMQuery

interface MapReferenceClient {
    suspend fun search(point: GeoPoint): OSMLocation?
    suspend fun search(query: OSMQuery): List<OSMLocation>?
    suspend fun search(city: String, state: String): List<OSMLocation>?
    suspend fun searchCity(query: String, country: String): List<OSMCity>?
    suspend fun readCity(city: String, state: String): OSMCity?
}
