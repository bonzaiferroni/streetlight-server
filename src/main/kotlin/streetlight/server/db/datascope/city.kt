package streetlight.server.db.datascope

import kampfire.model.HttpProblem
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.toDataOr
import klutch.db.model.CallerId
import streetlight.model.data.City
import streetlight.model.data.CityEdit
import streetlight.server.db.tables.CityTable
import streetlight.server.model.DataScope
import streetlight.server.routes.checkImageAndStore

suspend fun DataScope.updateCity(
    callerId: CallerId,
    edit: CityEdit,
): Outcome<City> = transaction {
    val name = edit.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@transaction HttpProblem.BadRequest
    if (dao.city.isNameTaken(edit.cityId, name)) return@transaction HttpProblem.Conflict
    val image = checkImageAndStore(callerId, edit.cityId, edit.image, CityTable.imageConfig)
        .toDataOr { return@transaction it }

    log("updating city: $name")
    val city = dao.city.update(edit.copy(name = name, image = image)) ?: return@transaction HttpProblem.NotFound
    Ok(city)
}
