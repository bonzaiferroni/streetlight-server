package streetlight.server.db.services

import klutch.db.DbService
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.CityId
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
import streetlight.server.db.tables.TaskTable
import streetlight.server.db.tables.SavedImageSet
import streetlight.server.db.tables.StarTable
import streetlight.server.model.DaoFacade
import kotlin.time.Clock
import kotlin.uuid.Uuid

class LocationService(val dao: DaoFacade): DbService() {
    suspend fun update(
        locationId: LocationId,
        cityId: CityId,
        callerId: StarId,
        edit: LocationEdit,
        imageSet: SavedImageSet?
    ) = dbQuery {
        val location = dao.location.update(
            locationId, cityId, callerId, edit, imageSet
        )
        val editLogId = dao.editLog.create(EditType.Update, edit, locationId, callerId)
    }

    suspend fun createWithTask(
        cityId: CityId,
        callerId: StarId,
        edit: LocationEdit,
        imageSet: SavedImageSet?
    ) = dbQuery {
        val location = dao.location.create(
            cityId, callerId, edit, imageSet
        ) ?: return@dbQuery null
        val editLogId = dao.editLog.create(EditType.Create, location.toEdit(), location.locationId, callerId)
        val star = dao.star.readStar(callerId) ?: error("star not found")
        val description = edit.description
        val needsReview = edit.imageRef == null || edit.website == null
                || description == null || description.length < 100 || star.scoutLevel == 0
        if (needsReview) {
            createEditTask(editLogId)
        }
        location
    }

    suspend fun createEditTask(editLogId: EditLogId) {
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

    suspend fun findUniverseScouts(limit: Int) = dbQuery {
        // td: select from pool of recently active users
        StarTable.leftJoin(TaskTable)
            .select(StarTable.id, TaskTable.createdAt.max())
            .where { StarTable.scoutLevel.greaterEq(1) }
            .groupBy(StarTable.id)
            .orderBy(TaskTable.createdAt.max() to SortOrder.ASC_NULLS_FIRST)
            .limit(limit)
            .map { StarId(it[StarTable.id].value) }
    }
}