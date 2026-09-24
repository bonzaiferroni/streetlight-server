package streetlight.server.db.datascope

import kampfire.model.CoreProblem
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.toDataOr
import klutch.db.model.CallerId
import streetlight.model.data.EditType
import streetlight.model.data.Location
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationId
import streetlight.model.data.toEdit
import streetlight.server.db.services.createEditTask
import streetlight.server.db.services.readOrCreateCity
import streetlight.server.db.tables.LocationTable
import streetlight.server.model.DataScope
import streetlight.server.routes.checkImageAndStore
import streetlight.server.utils.TimeZones
import streetlight.server.utils.toStarId

/**
 * Creates a location in its city, created when new, and logs the edit. The edit is sent for review when it asks
 * for one or the caller is a new scout.
 */
suspend fun DataScope.createLocation(
    callerId: CallerId,
    edit: LocationEdit,
): Outcome<Location> = transaction {
    val cityId = readOrCreateCity(edit.city, edit.state) ?: error("city not found: ${edit.city}")
    val preparedEdit = prepareLocation(callerId, edit).toDataOr { return@transaction it }

    log("creating location: ${preparedEdit.label}")
    val location = dao.location.create(cityId, callerId, preparedEdit) ?: return@transaction CoreProblem.Something
    val editLogId = dao.editLog.create(EditType.Create, location.toEdit(), location.locationId, callerId)
    val star = dao.star.readStar(callerId.toStarId()) ?: error("star not found")

    if (preparedEdit.needsReview || star.scoutLevel == 0) {
        createEditTask(editLogId)
    }
    Ok(location)
}

/** Updates a location in its city, created when new, and logs the edit. */
suspend fun DataScope.updateLocation(
    locationId: LocationId,
    callerId: CallerId,
    edit: LocationEdit,
): Outcome<Location> = transaction {
    val cityId = readOrCreateCity(edit.city, edit.state) ?: error("city not found: ${edit.city}")
    val preparedEdit = prepareLocation(callerId, edit).toDataOr { return@transaction it }

    log("updating location: ${preparedEdit.label}")
    val location = dao.location.update(locationId, cityId, callerId, preparedEdit)
        ?: return@transaction CoreProblem.Something
    dao.editLog.create(EditType.Update, preparedEdit, locationId, callerId)
    Ok(location)
}

/** [edit] with its image stored and its time zone found from its point. */
suspend fun DataScope.prepareLocation(callerId: CallerId, edit: LocationEdit): Outcome<LocationEdit> {
    val image = checkImageAndStore(callerId, edit.locationId, edit.image, LocationTable.imageConfig)
        .toDataOr { return it }
    val geoPoint = edit.geoPoint ?: error("GeoPoint not found")
    val timezoneId = TimeZones.zoneIdAt(geoPoint) ?: error("timezone query unsuccess")
    return Ok(edit.copy(image = image, timezoneId = timezoneId))
}

//private suspend fun <T> DataScope.handleEdit(
//    edit: LocationEdit,
//    identity: StarIdentity,
//    block: suspend (CityId, SavedImageSet?) -> T?
//): T? {
//    val imageUserId = identity.starId.takeIf { edit.imageRef?.isRelative ?: false }
//    val imageSet = saveImages(imageUserId, edit.locationId, edit.imageRef, EventTable.imageConfig)
//    val cityId = readOrCreateCity(edit.city, edit.state)
//    return block(requireNotNull(cityId) { "city not found" }, imageSet)
//}