package streetlight.server.e2e

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import streetlight.model.data.Feedback
import streetlight.server.db.tables.FeedbackTable
import streetlight.server.db.tables.toFeedback

fun latestFeedbackOrNull(): Feedback? = transaction {
    FeedbackTable.selectAll()
        .orderBy(FeedbackTable.createdAt to SortOrder.DESC)
        .limit(1)
        .map { it.toFeedback() }
        .singleOrNull()
}

fun latestDeviceAgentOrNull(): String? = transaction {
    FeedbackTable.selectAll()
        .orderBy(FeedbackTable.createdAt to SortOrder.DESC)
        .limit(1)
        .map { it[FeedbackTable.deviceAgent] }
        .singleOrNull()
}

fun feedbackCount(): Int = transaction {
    FeedbackTable.selectAll().count().toInt()
}
