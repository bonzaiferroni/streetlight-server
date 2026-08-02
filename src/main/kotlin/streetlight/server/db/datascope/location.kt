package streetlight.server.db.datascope

import klutch.db.model.CallerId
import streetlight.model.data.EditType
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationId
import streetlight.model.data.toEdit
import streetlight.server.db.services.createEditTask
import streetlight.server.db.services.readOrCreateCity
import streetlight.server.db.tables.LocationTable
import streetlight.server.model.DataScope
import streetlight.server.routes.checkImageAndStore
import streetlight.server.utils.toStarId

suspend fun DataScope.createLocation(
    callerId: CallerId,
    edit: LocationEdit,
) = transaction {
    val image = checkImageAndStore(callerId, edit.locationId, edit.image, LocationTable.imageConfig)
    val cityId = readOrCreateCity(edit.city, edit.state) ?: error("city not found: ${edit.city}")

    log("creating location: ${edit.label}")
    val location = dao.location.create(cityId, callerId, edit.copy(image = image)) ?: return@transaction null
    val editLogId = dao.editLog.create(EditType.Create, location.toEdit(), location.locationId, callerId)
    val star = dao.star.readStar(callerId.toStarId()) ?: error("star not found")

    if (edit.needsReview || star.scoutLevel == 0) {
        createEditTask(editLogId)
    }
    location
}

suspend fun DataScope.updateLocation(
    locationId: LocationId,
    callerId: CallerId,
    edit: LocationEdit,
) = transaction {
    val image = checkImageAndStore(callerId, edit.locationId, edit.image, LocationTable.imageConfig)
    val cityId = readOrCreateCity(edit.city, edit.state) ?: error("city not found: ${edit.city}")

    log("updating location: ${edit.label}")
    val location = dao.location.update(locationId, cityId, callerId, edit)
    val editLogId = dao.editLog.create(EditType.Update, edit.copy(image = image), locationId, callerId)
    location
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