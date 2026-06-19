package streetlight.server.db.services

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.insert
import streetlight.model.data.EditLog
import streetlight.model.data.EditLogId
import streetlight.model.data.EditType
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationId
import streetlight.model.data.RecordEdit
import streetlight.model.data.RecordId
import streetlight.model.data.RecordType
import streetlight.model.data.StarId
import streetlight.model.data.toRecordId
import streetlight.server.db.tables.EditLogTable
import streetlight.server.db.tables.createRecord
import kotlin.time.Clock
import kotlin.uuid.Uuid

fun EditLogTable.logEdit(editType: EditType, edit: RecordEdit, recordId: RecordId, callerId: StarId) {
    val editLog = edit.toEditLog(recordId, editType)
    insert {
        it.createRecord(editLog, callerId)
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
