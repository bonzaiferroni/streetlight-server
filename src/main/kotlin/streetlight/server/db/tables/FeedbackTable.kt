package streetlight.server.db.tables

import kampfire.api.toUsername
import klutch.db.SyncValueTrigger
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.*
import streetlight.server.utils.toRecordIdOrNull

object FeedbackTable: UuidTable("feedback") {
    val starId = reference("star_id", StarTable, ReferenceOption.SET_NULL).nullable().index()
    val username = text("username").nullable()
    val text = text("text")
    val feedbackType = enumeration<FeedbackType>("feedback_type")
    val platform = enumeration<Platform>("platform")
    val isPrivate = bool("is_private")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

val feedbackUsernameSync = SyncValueTrigger(FeedbackTable.starId, FeedbackTable.username, StarTable, StarTable.username)

fun ResultRow.toFeedback() = Feedback(
    feedbackId = FeedbackId(this[FeedbackTable.id].value),
    feedbackType = this[FeedbackTable.feedbackType],
    username = this[FeedbackTable.username]?.toUsername(),
    text = this[FeedbackTable.text],
    platform = this[FeedbackTable.platform],
    isPrivate = this[FeedbackTable.isPrivate],
    updatedAt = this[FeedbackTable.updatedAt],
    createdAt = this[FeedbackTable.createdAt],
)

fun UpdateBuilder<*>.writeFull(feedback: Feedback, callerId: StarId?) {
    this[FeedbackTable.id] = feedback.feedbackId.value
    this[FeedbackTable.starId] = callerId?.value
    this[FeedbackTable.createdAt] = feedback.createdAt
    writeUpdate(feedback)
}

fun UpdateBuilder<*>.writeUpdate(feedback: Feedback) {
    this[FeedbackTable.text] = feedback.text
    this[FeedbackTable.feedbackType] = feedback.feedbackType
    this[FeedbackTable.platform] = feedback.platform
    this[FeedbackTable.isPrivate] = feedback.isPrivate
    this[FeedbackTable.updatedAt] = feedback.updatedAt
}