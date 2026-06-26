package streetlight.server.routes

import kampfire.model.Ok
import klutch.server.provide
import klutch.server.getApi
import klutch.server.readParam
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.model.data.City
import streetlight.model.data.CityId
import streetlight.model.external.OSMCity
import streetlight.server.external.OSMHttpClient
import streetlight.server.log
import streetlight.server.model.ApiScope

fun ApiScope.serveCity() {
    val osm = provide<OSMHttpClient>()

    getApi(Api.Cities.Search) { endpoint ->
        val query = readParam(endpoint.query)
        val country = readParam(endpoint.country)
        val limit = readParamOrNull(endpoint.limit) ?: 10

        val localities = if (query.isBlank()) {
            dao.city.readTopCities()
        } else {
            val dbLocalities = dao.city.searchCities(query, limit)
            if (dbLocalities.isNotEmpty() || query.length < 2) {
                dbLocalities
            } else {
                ::serveCity.log("querying osm: $query")
                val cities = osm.searchCity(query, country)?.filter { city ->
                    dbLocalities.none { it.name == city.name && it.state == city.state && it.country == city.country }
                }?.map { it.toCity() }?.takeIf { it.isNotEmpty() }
                if (cities != null) {
                    val osmLocalities = dao.city.createCities(cities)
                    dbLocalities + osmLocalities.filter { it.name.startsWith(query, ignoreCase = true) }.take(limit - dbLocalities.size)
                } else {
                    dbLocalities
                }
            }
        }

        Ok(localities)
    }
}

fun OSMCity.toCity() = City(
    cityId = CityId.empty,
    name = name,
    state = state,
    country = country,
    galaxyCount = 0,
    mapRank = importance,
    geoPoint = geoPoint,
    geoBounds = geoBounds,
)