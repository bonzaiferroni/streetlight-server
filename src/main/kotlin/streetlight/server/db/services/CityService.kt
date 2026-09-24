package streetlight.server.db.services

import io.github.oshai.kotlinlogging.Level
import streetlight.model.data.CityId
import streetlight.server.model.DataScope
import streetlight.server.routes.toCity

/**
 * The id of the city [name] in [state], created from OpenStreetMap when new, or `null` when either is missing or
 * not found.
 */
suspend fun DataScope.readOrCreateCity(name: String?, state: String?): CityId? {
    val name = name ?: return null
    val state = state ?: return null

    dao.city.readCityId(name, state)?.let {
        return it
    }

    val osmCity = client.osm.readCity(name, state) ?: return null.also {
        log("osm unable to find city: $name, $state", Level.ERROR)
    }

    // td: handle multiple name/state cities
    val city = client.osm.readCity(name, state)?.toCity() ?: return null
    return dao.city.createCity(city)
}