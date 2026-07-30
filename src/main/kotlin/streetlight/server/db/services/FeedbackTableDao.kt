package streetlight.server.db.services

import kampfire.api.Username
import kampfire.model.UserRole
import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.db.model.Identity
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import streetlight.model.data.Feedback
import streetlight.model.data.FeedbackEdit
import streetlight.model.data.FeedbackId
import streetlight.model.data.StarId
import streetlight.server.db.tables.*
import kotlin.time.Clock

class FeedbackTableDao: DbService() {

    suspend fun create(edit: FeedbackEdit, callerId: CallerId? = null) = dbQuery {
        val record = edit.toRecord()
        FeedbackTable.insert { it.writeFull(record, callerId) }.insertedCount == 1
    }

    suspend fun update(feedback: Feedback) = dbQuery {
        FeedbackTable.update({ FeedbackTable.id.eq(feedback.feedbackId) }) {
            it.writeUpdate(feedback)
        }
    }

    suspend fun delete(feedbackId: FeedbackId) = dbQuery {
        FeedbackTable.deleteWhere { FeedbackTable.id.eq(feedbackId) }
    }

    suspend fun read(feedbackId: FeedbackId) = dbQuery {
        FeedbackTable.selectAll().where { FeedbackTable.id.eq(feedbackId) }
            .map { it.toFeedback() }.singleOrNull()
    }

    suspend fun feed(caller: Identity?) = dbQuery {
        FeedbackTable.selectAll().apply {
            if (caller != null && !caller.roles.contains(UserRole.Admin)) {
                andWhere { FeedbackTable.isPrivate.eq(false) }
            }
        }.orderBy(FeedbackTable.createdAt, SortOrder.DESC).limit(100)
            .map { it.toFeedback() }
    }
}

private fun FeedbackEdit.toRecord() = Feedback(
    feedbackId = FeedbackId.random(),
    feedbackType = feedbackType,
    username = Username.Empty, // set with trigger
    text = text,
    platform = platform,
    isPrivate = isPrivate,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)