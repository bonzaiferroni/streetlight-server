package streetlight.server.routes

import kampfire.model.Ok
import klutch.server.ApiContext
import klutch.server.getApi
import klutch.server.readParam
import streetlight.model.Api
import streetlight.model.data.Locality
import streetlight.model.external.OSMCity
import streetlight.server.external.OSMHttpClient
import streetlight.server.model.dao

fun ApiContext.serveLocality() {
    val osm = server.get<OSMHttpClient>()

    getApi(Api.Localities.SearchCity) { endpoint ->
        val query = readParam(endpoint.query)
        val localities = if (query.isBlank()) {
            dao.city.readTopCities()
        } else {
            val country = readParam(endpoint.country)
            val dbLocalities = dao.city.searchLocalities(query)
            if (dbLocalities.any { it.city.equals(query, ignoreCase = true) }) {
                dbLocalities
            } else {
                dbLocalities + (osm.searchCity(query, country)?.filter { it.name.startsWith(query, ignoreCase = true) }
                    ?.map { it.toLocality() } ?: emptyList())
            }
        }

        Ok(localities)
    }
}

fun OSMCity.toLocality() = Locality(
    cityId = null,
    city = name,
    state = state,
    country = country,
    galaxyCount = 0
)