package streetlight.server.model

import kampfire.model.GeoPoint
import streetlight.model.external.OSMCity
import streetlight.model.external.OSMLocation
import streetlight.model.external.OSMQuery

class TestMapReferenceClient: MapReferenceClient {
    override suspend fun search(point: GeoPoint): OSMLocation? {
        TODO("Not yet implemented")
    }

    override suspend fun search(query: OSMQuery): List<OSMLocation>? {
        TODO("Not yet implemented")
    }

    override suspend fun search(
        city: String,
        state: String
    ): List<OSMLocation>? {
        TODO("Not yet implemented")
    }

    override suspend fun searchCity(
        query: String,
        country: String
    ): List<OSMCity>? {
        TODO("Not yet implemented")
    }

    override suspend fun readCity(city: String, state: String): OSMCity? {
        TODO("Not yet implemented")
    }
}
