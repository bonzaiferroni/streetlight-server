package streetlight.server

import kampfire.api.Slug
import kampfire.model.GeoPoint
import kampfire.model.GeoRect
import klutch.db.model.CallerId
import streetlight.model.data.City
import streetlight.model.data.CityId
import streetlight.model.data.Location
import streetlight.model.data.LocationEdit
import streetlight.model.data.StarId

const val LOCATION_NAME = "The Fox Den"
val DENVER = GeoPoint(-104.99, 39.74)
val DENVER_AREA = GeoRect(GeoPoint(-105.1, 39.6), GeoPoint(-104.6, 39.9))

suspend fun TestServer.seedCity(): CityId = dao.city.createCity(
    City(
        cityId = CityId.random(),
        slug = Slug("denver-colorado"),
        name = "Denver",
        state = "Colorado",
        country = "United States",
        galaxyCount = 0,
        locationCount = 0,
        mapRank = null,
        geoPoint = DENVER,
        geoRect = DENVER_AREA,
    )
)

suspend fun TestServer.seedLocation(scoutId: StarId, name: String = LOCATION_NAME): Location {
    val cityId = dao.city.readCityId("Denver", "Colorado") ?: seedCity()
    return dao.location.create(cityId, CallerId(scoutId.value), locationEdit(name).copy(timezoneId = "America/Denver"))
        ?: error("location was not created: $name")
}

fun locationEdit(name: String = LOCATION_NAME) = LocationEdit(
    name = name,
    city = "Denver",
    state = "Colorado",
    geoPoint = DENVER,
)
