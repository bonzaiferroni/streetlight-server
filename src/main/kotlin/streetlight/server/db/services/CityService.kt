package streetlight.server.db.services

import streetlight.model.data.CityId
import streetlight.server.external.OSMHttpClient
import streetlight.server.model.DaoScope
import streetlight.server.model.DataScope
import streetlight.server.model.dao
import streetlight.server.routes.toCity
import kotlin.reflect.KFunction

suspend fun DataScope.readOrCreateCity(name: String?, state: String?): CityId? {
    val name = name ?: return null
    val state = state ?: return null

    dao.city.readCityId(name, state)?.let {
        return it
    }

    val city = client.osm.readCity(name, state)?.toCity() ?: return null.also {
        log("osm unable to find city: $name, $state")
    }
    return dao.city.createCity(city)
}