package streetlight.server.routes

import kampfire.api.Slug
import kampfire.model.Ok
import kampfire.model.toOutcome
import klutch.server.authGate
import klutch.server.provide
import klutch.server.getApi
import klutch.server.readParam
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.model.data.City
import streetlight.model.data.CityId
import streetlight.model.external.OSMCity
import streetlight.server.model.MapReferenceClient
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentityOrNull

fun ApiScope.serveCity() {
    val osm = provide<MapReferenceClient>()

    getApi(Api.Cities.ReadTopCities) {
        dao.city.readTopCities().toOutcome()
    }

    getApi(Api.Cities.ReadCity) {
        dao.city.readCity(it.data).toOutcome()
    }

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
                log("querying osm: $query")
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

    authGate(optional = true) {
        getApi(Api.Cities.ReadCityPosts) {
            val identity = call.getIdentityOrNull()

            dao.city.readCityPosts(it.data, identity?.callerId).toOutcome()
        }
    }
}

fun OSMCity.toCity() = City(
    cityId = CityId.random(),
    slug = Slug.Empty,
    name = name,
    state = state,
    country = country,
    galaxyCount = 0,
    locationCount = 0,
    mapRank = importance,
    geoPoint = geoPoint,
    geoRect = geoRect,
)