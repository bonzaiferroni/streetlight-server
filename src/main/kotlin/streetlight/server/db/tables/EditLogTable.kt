package streetlight.server.db.tables

import klutch.db.SyncValueTrigger
import klutch.db.model.CallerId
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.EditLog
import streetlight.model.data.EditType
import streetlight.model.data.RecordType
import streetlight.model.data.RecordEdit
import streetlight.model.data.StarId
import kotlin.time.Clock

object EditLogTable: UuidTable("edit_log") {
    val recordId = uuid("record_id")
    val starId = reference("star_id", StarTable, ReferenceOption.SET_NULL).index().nullable()
    val username = text("username").index().default("")
    val recordType = enumeration<RecordType>("record_type")
    val recordEdit = jsonb<RecordEdit>("record_edit", tableJsonDefault).nullable()
    val editType = enumeration<EditType>("edit_type")
    // val note = text("note").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

val editUsernameSync = SyncValueTrigger(EditLogTable.starId, EditLogTable.username, StarTable, StarTable.username)

fun UpdateBuilder<*>.createRecord(editLog: EditLog, callerId: CallerId) {
    this[EditLogTable.id] = editLog.editLogId.value
    this[EditLogTable.recordId] = editLog.recordId
    this[EditLogTable.starId] = callerId.value
    this[EditLogTable.recordType] = editLog.recordType
    this[EditLogTable.editType] = editLog.editType
    this[EditLogTable.createdAt] = Clock.System.now()
    updateRecord(editLog)
}

fun UpdateBuilder<*>.updateRecord(editLog: EditLog) {
    this[EditLogTable.recordEdit] = editLog.recordEdit
    this[EditLogTable.updatedAt] = Clock.System.now()
}