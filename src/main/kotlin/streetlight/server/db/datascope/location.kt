package streetlight.server.db.datascope

import kampfire.model.CoreProblem
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.toDataOr
import klutch.db.model.CallerId
import streetlight.model.data.EditType
import streetlight.model.data.Location
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationId
import streetlight.model.data.toEdit
import streetlight.model.utils.expandAddress
import streetlight.server.db.services.createEditTask
import streetlight.server.db.services.readOrCreateCity
import streetlight.server.db.tables.LocationTable
import streetlight.server.model.DataScope
import streetlight.server.routes.checkImageAndStore
import streetlight.server.utils.TimeZones
import streetlight.server.utils.toStarId

/**
 * Creates a location in its city when it names one, the city created when new, and logs the edit of a caller. The edit is sent for review
 * when it asks for one or the caller is a new scout. Fails when its image cannot be stored and [isImageRequired];
 * otherwise the location is created without the image.
 */
suspend fun DataScope.createLocation(
    callerId: CallerId?,
    edit: LocationEdit,
    isImageRequired: Boolean = true,
): Outcome<Location> = transaction {
    val cityId = edit.city?.let { readOrCreateCity(it, edit.state) ?: error("city not found: $it") }
    val preparedEdit = prepareLocation(callerId, edit, isImageRequired).toDataOr { return@transaction it }

    log("creating location: ${preparedEdit.label}")
    val location = dao.location.create(cityId, callerId, preparedEdit) ?: return@transaction CoreProblem.Something
    callerId ?: return@transaction Ok(location)
    val editLogId = dao.editLog.create(EditType.Create, location.toEdit(), location.locationId, callerId)
    val star = dao.star.readStar(callerId.toStarId()) ?: error("star not found")

    if (preparedEdit.needsReview || star.scoutLevel == 0) {
        createEditTask(editLogId)
    }
    Ok(location)
}

/**
 * Updates a location in its city when it names one, the city created when new, and logs the edit of a caller. With
 * no caller, only a location without a host is updated. Fails when its image cannot be stored and [isImageRequired];
 * otherwise the location is updated without the image.
 */
suspend fun DataScope.updateLocation(
    locationId: LocationId,
    callerId: CallerId?,
    edit: LocationEdit,
    isImageRequired: Boolean = true,
): Outcome<Location> = transaction {
    val cityId = edit.city?.let { readOrCreateCity(it, edit.state) ?: error("city not found: $it") }
    val preparedEdit = prepareLocation(callerId, edit, isImageRequired).toDataOr { return@transaction it }

    log("updating location: ${preparedEdit.label}")
    val location = dao.location.update(locationId, cityId, callerId, preparedEdit)
        ?: return@transaction CoreProblem.Something
    callerId?.let { dao.editLog.create(EditType.Update, preparedEdit, locationId, it) }
    Ok(location)
}

/**
 * [edit] with its image stored, its address expanded and its time zone found from its point, or without the image
 * when it cannot be stored and not [isImageRequired].
 */
suspend fun DataScope.prepareLocation(
    callerId: CallerId?,
    edit: LocationEdit,
    isImageRequired: Boolean = true,
): Outcome<LocationEdit> {
    val image = when (val stored = checkImageAndStore(callerId, edit.locationId, edit.image, LocationTable.imageConfig)) {
        is Ok -> stored.data
        is Problem -> if (isImageRequired) return stored else null
    }
    val geoPoint = edit.geoPoint ?: error("GeoPoint not found")
    val timezoneId = TimeZones.zoneIdAt(geoPoint) ?: error("timezone query unsuccess")
    return Ok(edit.copy(image = image, address = edit.address?.expandAddress(), timezoneId = timezoneId))
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