package streetlight.server.db.services

import klutch.db.DbService
import org.jetbrains.exposed.v1.jdbc.insert
import streetlight.model.data.Review
import streetlight.server.db.tables.ReviewTable
import streetlight.server.db.tables.writeFull

class ReviewTableDao(): DbService() {
    suspend fun create(review: Review) = dbQuery {
        ReviewTable.insert {
            it.writeFull(review)
        }
    }
}