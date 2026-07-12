package streetlight.server.db.services

import kampfire.model.CallerId
import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Alias
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import streetlight.model.data.EditLog
import streetlight.model.data.EditLogId
import streetlight.model.data.EditType
import streetlight.model.data.RecordEdit
import streetlight.model.data.RecordId
import streetlight.server.db.tables.EditLogTable
import streetlight.server.db.tables.createRecord
import kotlin.time.Clock
import kotlin.uuid.Uuid

class EditLogTableDao: DbService() {

    suspend fun create(editType: EditType, edit: RecordEdit, recordId: RecordId, callerId: CallerId) = dbQuery {
        val editLog = edit.toEditLog(recordId, editType)
        val editLogId = EditLogTable.insertAndGetId {
            it.createRecord(editLog, callerId)
        }.value.let { EditLogId(it) }
        editLogId
    }

    suspend fun readEdits(recordId: RecordId) = dbQuery {
        EditLogTable.read { it.recordId.eq(recordId.value) }.map { it.toEditLog() }
    }

    suspend fun readEdits(callerId: CallerId) = dbQuery {
        EditLogTable.read { it.starId.eq(callerId) }.map { it.toEditLog() }
    }
}

fun RecordEdit.toEditLog(recordId: RecordId, editType: EditType) = EditLog(
    editLogId = EditLogId(Uuid.random()),
    recordId = recordId.value,
    username = "", // set by trigger
    recordType = recordType,
    recordEdit = this,
    editType = editType,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)

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

fun ResultRow.toEditLog(alias: Alias<EditLogTable>? = null): EditLog {
    fun <T> col(column: Column<T>) = alias?.get(column) ?: column

    return EditLog(
        editLogId = EditLogId(this[col(EditLogTable.id)].value),
        recordId = this[col(EditLogTable.recordId)],
        username = this[col(EditLogTable.username)],
        recordType = this[col(EditLogTable.recordType)],
        recordEdit = this[col(EditLogTable.recordEdit)],
        editType = this[col(EditLogTable.editType)],
        updatedAt = this[col(EditLogTable.updatedAt)],
        createdAt = this[col(EditLogTable.createdAt)],
    )
}