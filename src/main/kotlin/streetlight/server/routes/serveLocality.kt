package streetlight.server.routes

import kampfire.model.Ok
import klutch.server.ApiContext
import klutch.server.getApi
import klutch.server.readParam
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.model.data.CityId
import streetlight.model.data.Locality
import streetlight.model.external.OSMCity
import streetlight.server.external.OSMHttpClient
import streetlight.server.model.console
import streetlight.server.model.dao

fun ApiContext.serveLocality() {
    val osm = server.get<OSMHttpClient>()

    getApi(Api.Localities.SearchCity) { endpoint ->
        val query = readParam(endpoint.query)
        val country = readParam(endpoint.country)
        val limit = readParamOrNull(endpoint.limit) ?: 10

        val localities = if (query.isBlank()) {
            dao.city.readTopCities()
        } else {
            val dbLocalities = dao.city.searchLocalities(query, limit)
            if (dbLocalities.size == limit || query.length < 2) {
                dbLocalities
            } else {
                console.log("querying osm: $query")
                val cities = osm.searchCity(query, country)?.filter { city ->
                    dbLocalities.none { it.city == city.name && it.state == city.state && it.country == city.country }
                }?.map { it.toLocality() }?.takeIf { it.isNotEmpty() }
                if (cities != null) {
                    val osmLocalities = dao.city.createCities(cities)
                    dbLocalities + osmLocalities.filter { it.city.startsWith(query, ignoreCase = true) }.take(limit - dbLocalities.size)
                } else {
                    dbLocalities
                }
            }
        }

        Ok(localities)
    }
}

fun OSMCity.toLocality() = Locality(
    cityId = CityId.empty,
    city = name,
    state = state,
    country = country,
    galaxyCount = 0
)