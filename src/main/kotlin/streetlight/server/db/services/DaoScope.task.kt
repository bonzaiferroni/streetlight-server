package streetlight.server.db.services

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.BaseTask
import streetlight.model.data.EditLogId
import streetlight.model.data.RecordType
import streetlight.model.data.StarId
import streetlight.model.data.TaskId
import streetlight.model.data.TaskStatus
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.TaskTable
import streetlight.server.model.DaoScope
import kotlin.time.Clock
import kotlin.uuid.Uuid

/** Asks the scout who has waited longest for a task to review the edit [editLogId]. */
suspend fun DaoScope.createEditTask(editLogId: EditLogId) {
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