package streetlight.server.service

import io.github.oshai.kotlinlogging.Level
import kampfire.model.HttpProblem
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.toDataOr
import klutch.db.model.CallerId
import streetlight.model.data.City
import streetlight.model.data.CityEdit
import streetlight.model.data.CityId
import streetlight.server.db.tables.CityTable
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

/** Updates a city with [edit], storing a new image. Fails for a blank or taken name. */
suspend fun DataScope.updateCity(
    callerId: CallerId,
    edit: CityEdit,
): Outcome<City> = transaction {
    val name = edit.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@transaction HttpProblem.BadRequest
    if (dao.city.isNameTaken(edit.cityId, name)) return@transaction HttpProblem.Conflict
    val image = checkImageAndStore(callerId, edit.cityId, edit.image, CityTable)
        .toDataOr { return@transaction it }

    log("updating city: $name")
    val city = dao.city.update(edit.copy(name = name, image = image)) ?: return@transaction HttpProblem.NotFound
    Ok(city)
}
