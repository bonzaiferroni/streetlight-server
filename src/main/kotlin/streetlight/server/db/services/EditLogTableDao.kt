package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import streetlight.model.data.EditLog
import streetlight.model.data.EditLogId
import streetlight.model.data.EditType
import streetlight.model.data.LocationEdit
import streetlight.model.data.RecordEdit
import streetlight.model.data.RecordId
import streetlight.model.data.RecordType
import streetlight.model.data.StarId
import streetlight.server.db.tables.EditLogTable
import streetlight.server.db.tables.createRecord
import kotlin.time.Clock
import kotlin.uuid.Uuid

class EditLogTableDao: DbService() {

    suspend fun create(editType: EditType, edit: RecordEdit, recordId: RecordId, callerId: StarId) = dbQuery {
        val editLog = edit.toEditLog(recordId, editType)
        val editLogId = EditLogTable.insertAndGetId {
            it.createRecord(editLog, callerId)
        }.value.let { EditLogId(it) }
        editLogId
    }

    suspend fun readEdits(recordId: RecordId) = dbQuery {
        EditLogTable.read { it.recordId.eq(recordId.value) }.map { it.toEditLog() }
    }

    suspend fun readEdits(starId: StarId) = dbQuery {
        EditLogTable.read { it.starId.eq(starId) }.map { it.toEditLog() }
    }
}

fun RecordEdit.toEditLog(recordId: RecordId, editType: EditType) = when(this) {
    is LocationEdit -> EditLog(
        editLogId = EditLogId(Uuid.random()),
        recordId = recordId.value,
        username = "", // set by trigger
        recordType = RecordType.Location,
        recordEdit = this,
        editType = editType,
        updatedAt = Clock.System.now(),
        createdAt = Clock.System.now(),
    )
}

fun ResultRow.toEditLog() = EditLog(
    editLogId = EditLogId(this[EditLogTable.id].value),
    recordId = this[EditLogTable.recordId],
    username = this[EditLogTable.username],
    recordType = this[EditLogTable.recordType],
    recordEdit = this[EditLogTable.recordEdit],
    editType = this[EditLogTable.editType],
    updatedAt = this[EditLogTable.updatedAt],
    createdAt = this[EditLogTable.createdAt],
)
