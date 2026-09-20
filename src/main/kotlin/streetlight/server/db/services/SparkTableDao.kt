package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.Performer
import streetlight.model.data.PerformerId
import streetlight.model.data.StarId
import streetlight.model.data.toRecordId
import streetlight.server.db.tables.PerformerTable
import streetlight.server.db.tables.toSpark
import streetlight.server.db.tables.createPerformer
import streetlight.server.db.tables.updatePerformer

class SparkTableDao: DbService() {

    // Spark CRUD
    suspend fun readById(performerId: PerformerId) = dbQuery {
        PerformerTable.read { it.id.eq(performerId) }.firstOrNull()?.toSpark()
    }

    suspend fun readByUserId(userId: StarId) = dbQuery {
        PerformerTable.read { it.starId.eq(userId) }.firstOrNull()?.toSpark()
    }

    suspend fun createSpark(performer: Performer): PerformerId = dbQuery {
        PerformerTable.insertAndGetId {
            it.createPerformer(performer)
        }.value.toRecordId()
    }

    suspend fun updateSpark(performer: Performer) = dbQuery {
        PerformerTable.update(where = { PerformerTable.id.eq(performer.performerId) }) {
            it.updatePerformer(performer)
        } == 1
    }
}
