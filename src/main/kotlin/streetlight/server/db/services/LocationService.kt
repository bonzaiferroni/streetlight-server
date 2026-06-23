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
import streetlight.model.data.Question
import streetlight.model.data.Quorum
import streetlight.model.data.QuorumId
import streetlight.model.data.RecordType
import streetlight.model.data.BaseTask
import streetlight.model.data.TaskId
import streetlight.model.data.StarId
import streetlight.model.data.TaskStatus
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

    suspend fun create(
        cityId: CityId,
        callerId: StarId,
        edit: LocationEdit,
        imageSet: SavedImageSet?
    ) = dbQuery {
        val location = dao.location.create(
            cityId, callerId, edit, imageSet
        ) ?: return@dbQuery null
        val editLogId = dao.editLog.create(EditType.Create, edit, location.locationId, callerId)
        val question = Question.ValidLocation
        val now = Clock.System.now()
        val quorum = Quorum(
            quorumId = QuorumId(Uuid.random()),
            recordId = editLogId.value,
            recordType = RecordType.EditLog,
            question = question,
            decision = null,
            updatedAt = now,
            createdAt = now,
        )
        val quorumId = dao.quorum.create(quorum)
        val reviewerIds = findUniverseScouts(question.minSize)
        reviewerIds.forEach { reviewerId ->
            val reviewTask = BaseTask(
                taskId = TaskId(Uuid.random()),
                recordId = quorumId.value,
                recordType = RecordType.Quorum,
                starId = reviewerId,
                decision = null,
                taskStatus = TaskStatus.Requested,
                updatedAt = now,
                createdAt = now
            )
            dao.review.create(reviewTask)
            val editTask = BaseTask(
                taskId = TaskId(Uuid.random()),
                recordId = editLogId.value,
                recordType = RecordType.EditLog,
                starId = reviewerId,
                decision = null,
                taskStatus = TaskStatus.Requested,
                updatedAt = now,
                createdAt = now
            )
            dao.review.create(editTask)
        }
        location
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