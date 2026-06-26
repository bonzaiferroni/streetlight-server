package streetlight.server.db.services

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.EditType
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationId
import streetlight.model.data.RecordType
import streetlight.model.data.BaseTask
import streetlight.model.data.EditLogId
import streetlight.model.data.TaskId
import streetlight.model.data.StarId
import streetlight.model.data.TaskStatus
import streetlight.model.data.toEdit
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.TaskTable
import streetlight.server.db.tables.StarTable
import streetlight.server.model.DaoScope
import streetlight.server.model.DataScope
import streetlight.server.routes.saveImages
import kotlin.time.Clock
import kotlin.uuid.Uuid

suspend fun DataScope.createLocation(
    callerId: StarId,
    edit: LocationEdit,
) = transaction {
    val imageSet = saveImages(callerId, edit.locationId, edit.imageRef, LocationTable.imageConfig)
    val cityId = readOrCreateCity(edit.city, edit.state) ?: error("city not found: ${edit.city}")

    log("creating location: ${edit.label}")
    val location = dao.location.create(
        cityId, callerId, edit, imageSet
    ) ?: return@transaction null
    val editLogId = dao.editLog.create(EditType.Create, location.toEdit(), location.locationId, callerId)
    val star = dao.star.readStar(callerId) ?: error("star not found")

    if (edit.needsReview || star.scoutLevel == 0) {
        createEditTask(editLogId)
    }
    location
}

suspend fun DataScope.updateLocation(
    locationId: LocationId,
    callerId: StarId,
    edit: LocationEdit,
) = transaction {
    val imageSet = saveImages(callerId, edit.locationId, edit.imageRef, LocationTable.imageConfig)
    val cityId = readOrCreateCity(edit.city, edit.state) ?: error("city not found: ${edit.city}")

    log("updating location: ${edit.label}")
    val location = dao.location.update(
        locationId, cityId, callerId, edit, imageSet
    )
    val editLogId = dao.editLog.create(EditType.Update, edit, locationId, callerId)
    location
}

private suspend fun DaoScope.createEditTask(editLogId: EditLogId) {
    val now = Clock.System.now()
    val reviewerIds = findUniverseScouts(1)
    reviewerIds.forEach { reviewerId ->
        val editTask = BaseTask(
            taskId = TaskId(Uuid.random()),
            recordId = editLogId.value,
            recordType = RecordType.EditLog,
            // starId = starIds.random(),
            starId = reviewerId,
            decision = null,
            taskStatus = TaskStatus.Requested,
            updatedAt = now,
            createdAt = now
        )
        dao.review.create(editTask)
    }
}

private suspend fun DaoScope.findUniverseScouts(limit: Int) = transaction {
    // td: select from pool of recently active users
    StarTable.leftJoin(TaskTable)
        .select(StarTable.id, TaskTable.createdAt.max())
        .where { StarTable.scoutLevel.greaterEq(1) }
        .groupBy(StarTable.id)
        .orderBy(TaskTable.createdAt.max() to SortOrder.ASC_NULLS_FIRST)
        .limit(limit)
        .map { StarId(it[StarTable.id].value) }
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