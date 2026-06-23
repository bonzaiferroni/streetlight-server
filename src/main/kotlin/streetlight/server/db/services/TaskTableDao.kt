package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.explainAnalyze
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.model.data.EditLogId
import streetlight.model.data.QuorumReviewContent
import streetlight.model.data.RecordType
import streetlight.model.data.BaseTask
import streetlight.model.data.EditTask
import streetlight.model.data.EditTaskContent
import streetlight.model.data.QuorumTask
import streetlight.model.data.TaskId
import streetlight.model.data.StarId
import streetlight.server.db.tables.EditLogTable
import streetlight.server.db.tables.QuorumTable
import streetlight.server.db.tables.TaskTable
import streetlight.server.db.tables.toQuorum
import streetlight.server.db.tables.writeFull

class TaskTableDao(): DbService() {
    suspend fun create(task: BaseTask) = dbQuery {
        TaskTable.insert {
            it.writeFull(task)
        }
    }

    suspend fun readCallerTasks(callerId: StarId) = dbQuery {
        TaskTable
            .join(
                EditLogTable, JoinType.LEFT, TaskTable.recordId, EditLogTable.id,
                additionalConstraint = { TaskTable.recordType.eq(RecordType.EditLog) }
            )
            .join(
                QuorumTable, JoinType.LEFT, TaskTable.recordId, QuorumTable.id,
                additionalConstraint = { TaskTable.recordType.eq(RecordType.Quorum) }
            )
            .join(
                quorumEditLogAlias, JoinType.LEFT, QuorumTable.recordId, quorumEditLogAlias[EditLogTable.id],
                additionalConstraint = { QuorumTable.recordType.eq(RecordType.EditLog) }
            )
            .selectAll()
            .where { TaskTable.starId.eq(callerId) }
            .explainAnalyze()
            .map { it.toTaskContent() }
    }

    suspend fun readQuorumReview(taskId: TaskId) = dbQuery {
        val row = TaskTable.leftJoin(QuorumTable).selectAll()
            .where { TaskTable.id.eq(taskId) }
            .firstOrNull() ?: return@dbQuery null

        val quorum = row.toQuorum()
        val editLogId = EditLogId(quorum.recordId)
        val editLog = EditLogTable.selectAll()
            .where { EditLogTable.id.eq(editLogId) }
            .firstOrNull()?.toEditLog() ?: return@dbQuery null
        QuorumReviewContent(quorum = quorum, task = row.toReviewTask(), editLog = editLog)
    }
}

fun ResultRow.toTaskContent() = when(this[TaskTable.recordType]) {
    RecordType.Quorum -> toQuorumTask()
    RecordType.EditLog -> EditTaskContent(toEditTask(), toEditLog())
    else -> throw NotImplementedError()
}

fun ResultRow.toQuorumTask() = when(this[QuorumTable.recordType]) {
    RecordType.EditLog -> QuorumReviewContent(toQuorum(), toReviewTask(), toEditLog(quorumEditLogAlias))
    else -> throw NotImplementedError()
}

fun ResultRow.toReviewTask() = QuorumTask(
    taskId = TaskId(this[TaskTable.id].value),
    recordId = this[TaskTable.recordId],
    starId = StarId(this[TaskTable.starId].value),
    decision = this[TaskTable.decision],
    taskStatus = this[TaskTable.status],
    updatedAt = this[TaskTable.updatedAt],
    createdAt = this[TaskTable.createdAt]
)

fun ResultRow.toEditTask() = EditTask(
    taskId = TaskId(this[TaskTable.id].value),
    recordId = this[TaskTable.recordId],
    starId = StarId(this[TaskTable.starId].value),
    taskStatus = this[TaskTable.status],
    updatedAt = this[TaskTable.updatedAt],
    createdAt = this[TaskTable.createdAt]
)

fun ResultRow.toBaseTask() = BaseTask(
    taskId = TaskId(this[TaskTable.id].value),
    recordId = this[TaskTable.recordId],
    recordType = this[TaskTable.recordType],
    starId = StarId(this[TaskTable.starId].value),
    decision = this[TaskTable.decision],
    taskStatus = this[TaskTable.status],
    updatedAt = this[TaskTable.updatedAt],
    createdAt = this[TaskTable.createdAt]
)

val taskEditLogAlias = EditLogTable.alias("task_edit_log")
val quorumEditLogAlias = EditLogTable.alias("quorum_edit_log")

// fun ResultRow.toPost() = when (this[PostTable.postType]) {
//    PostType.Event -> toEventPost()
//    PostType.Location -> toLocationPost()
//    PostType.Content -> toBasicPost()
//}