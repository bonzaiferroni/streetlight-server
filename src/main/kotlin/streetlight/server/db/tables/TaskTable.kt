package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.QuorumId
import streetlight.model.data.RecordType
import streetlight.model.data.BaseTask
import streetlight.model.data.TaskId
import streetlight.model.data.StarId
import streetlight.model.data.TaskStatus
import kotlin.time.Clock

/** The review tasks asked of each star. */
object TaskTable : UuidTable("task") {
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val recordId = uuid("record_id").index()
    val recordType = enumeration<RecordType>("record_type")
    val status = enumeration<TaskStatus>("status")
    val decision = integer("decision").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun UpdateBuilder<*>.createTask(task: BaseTask) {
    this[TaskTable.id] = task.taskId.value
    this[TaskTable.recordId] = task.recordId
    this[TaskTable.recordType] = task.recordType
    this[TaskTable.starId] = task.starId.value
    this[TaskTable.createdAt] = task.createdAt
    updateTask(task)
}

fun UpdateBuilder<*>.updateTask(task: BaseTask) {
    this[TaskTable.decision] = task.decision
    this[TaskTable.status] = task.taskStatus
    this[TaskTable.updatedAt] = Clock.System.now()
}

fun ResultRow.toReview() = BaseTask(
    taskId = TaskId(this[TaskTable.id].value),
    recordId = this[TaskTable.recordId],
    recordType = this[TaskTable.recordType],
    starId = StarId(this[TaskTable.starId].value),
    decision = this[TaskTable.decision],
    taskStatus = this[TaskTable.status],
    updatedAt = this[TaskTable.updatedAt],
    createdAt = this[TaskTable.createdAt]
)
