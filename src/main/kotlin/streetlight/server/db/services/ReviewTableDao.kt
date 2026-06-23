package streetlight.server.db.services

import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.model.data.QuorumReview
import streetlight.model.data.EditLogId
import streetlight.model.data.EditReview
import streetlight.model.data.Review
import streetlight.model.data.ReviewId
import streetlight.model.data.StarId
import streetlight.server.db.tables.EditLogTable
import streetlight.server.db.tables.QuorumTable
import streetlight.server.db.tables.ReviewTable
import streetlight.server.db.tables.toQuorum
import streetlight.server.db.tables.toReview
import streetlight.server.db.tables.writeFull

class ReviewTableDao(): DbService() {
    suspend fun create(review: Review) = dbQuery {
        ReviewTable.insert {
            it.writeFull(review)
        }
    }

    suspend fun readQuorumReviews(callerId: StarId) = dbQuery {
        // ReviewTable.leftJoin(QuorumTable).selectAll()
        //     .where { ReviewTable.starId.eq(callerId) }.map {
        //         QuorumReview(quorum = it.toQuorum(), review = it.toReview())
        //     }

        ReviewTable
            .leftJoin(QuorumTable)
            .join(EditLogTable, JoinType.LEFT, QuorumTable.recordId, EditLogTable.id)
            .selectAll()
            .where { ReviewTable.starId.eq(callerId) }
            .map { EditReview(quorum = it.toQuorum(), review = it.toReview(), editLog = it.toEditLog()) }
    }

    suspend fun readQuorumReview(reviewId: ReviewId) = dbQuery {
        val row = ReviewTable.leftJoin(QuorumTable).selectAll()
            .where { ReviewTable.id.eq(reviewId) }
            .firstOrNull() ?: return@dbQuery null

        val quorum = row.toQuorum()
        val editLogId = EditLogId(quorum.recordId)
        val editLog = EditLogTable.selectAll()
            .where { EditLogTable.id.eq(editLogId) }
            .firstOrNull()?.toEditLog() ?: return@dbQuery null
        EditReview(quorum = quorum, review = row.toReview(), editLog = editLog)
    }
}