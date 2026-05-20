package streetlight.server.db.services

import kabinet.console.globalConsole
import streetlight.model.data.CityId
import streetlight.server.external.OSMHttpClient
import streetlight.server.model.DaoFacade
import streetlight.server.routes.toCity

private val console = globalConsole.getHandle(CityService::class)

class CityService(
    private val dao: DaoFacade,
    private val osm: OSMHttpClient
) {
    suspend fun readOrCreateCity(name: String?, state: String?): CityId? {
        val name = name ?: return null
        val state = state ?: return null
        dao.city.readCityId(name, state)?.let {
            return it
        }

        val city = osm.readCity(name, state)?.toCity() ?: return null.also {
            console.log("osm unable to find city: $name, $state")
        }
        return dao.city.createCity(city)
    }
}