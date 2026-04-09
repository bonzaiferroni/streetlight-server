package streetlight.server.db.services

import kampfire.model.BasicUserId
import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import klutch.utils.toStringId
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.Performer
import streetlight.model.data.PerformerId
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.PerformerTable
import streetlight.server.db.tables.toSpark
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate

class SparkTableDao: DbService() {

    // Spark CRUD
    suspend fun readById(performerId: PerformerId) = dbQuery {
        PerformerTable.read { it.id.eq(performerId) }.firstOrNull()?.toSpark()
    }

    suspend fun readByUserId(userId: BasicUserId) = dbQuery {
        PerformerTable.read { it.starId.eq(userId) }.firstOrNull()?.toSpark()
    }

    suspend fun createSpark(performer: Performer): PerformerId = dbQuery {
        PerformerTable.insertAndGetId {
            it.writeFull(performer)
        }.value.toStringId().toProjectId()
    }

    suspend fun updateSpark(performer: Performer) = dbQuery {
        PerformerTable.update(where = { PerformerTable.id.eq(performer.performerId) }) {
            it.writeUpdate(performer)
        } == 1
    }
}
