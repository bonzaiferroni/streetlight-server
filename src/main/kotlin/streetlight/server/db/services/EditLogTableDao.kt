package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.eq
import streetlight.model.data.RecordId
import streetlight.model.data.StarId
import streetlight.server.db.tables.EditLogTable

class EditLogTableDao: DbService() {
    suspend fun readEdits(recordId: RecordId) = dbQuery {
        EditLogTable.read { it.recordId.eq(recordId.value) }.map { it.toEditLog() }
    }

    suspend fun readEdits(starId: StarId) = dbQuery {
        EditLogTable.read { it.starId.eq(starId) }.map { it.toEditLog() }
    }
}