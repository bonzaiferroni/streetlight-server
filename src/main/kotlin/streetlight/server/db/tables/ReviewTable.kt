package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.QuorumId
import streetlight.model.data.RecordType
import streetlight.model.data.Review
import streetlight.model.data.ReviewId
import streetlight.model.data.StarId
import streetlight.model.data.TaskStatus
import kotlin.time.Clock

object ReviewTable : UuidTable("review") {
    val quorumId = reference("quorum_id", QuorumTable, onDelete = ReferenceOption.CASCADE)
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE)
    val decision = integer("decision").nullable()
    val taskStatus = enumeration<TaskStatus>("task_status")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun UpdateBuilder<*>.writeFull(review: Review) {
    this[ReviewTable.id] = review.reviewId.value
    this[ReviewTable.quorumId] = review.quorumId.value
    this[ReviewTable.starId] = review.starId.value
    this[ReviewTable.createdAt] = review.createdAt
    writeUpdate(review)
}

fun UpdateBuilder<*>.writeUpdate(review: Review) {
    this[ReviewTable.decision] = review.decision
    this[ReviewTable.taskStatus] = review.taskStatus
    this[ReviewTable.updatedAt] = Clock.System.now()
}

fun ResultRow.toReview() = Review(
    reviewId = ReviewId(this[ReviewTable.id].value),
    quorumId = QuorumId(this[ReviewTable.quorumId].value),
    starId = StarId(this[ReviewTable.starId].value),
    decision = this[ReviewTable.decision],
    taskStatus = this[ReviewTable.taskStatus],
    updatedAt = this[ReviewTable.updatedAt],
    createdAt = this[ReviewTable.createdAt]
)
