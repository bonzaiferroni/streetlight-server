package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.eq
import streetlight.model.data.EditType
import streetlight.model.data.RecordId
import streetlight.server.db.tables.EditLogTable

class EditLogTableDao: DbService() {
    suspend fun readEdits(recordId: RecordId) = dbQuery {
        EditLogTable.read { it.recordId.eq(recordId.value) }.map { it.toEditLog() }
    }
}