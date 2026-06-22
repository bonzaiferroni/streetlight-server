package streetlight.server.db.services

import klutch.db.DbService
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
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

//class EditLogTableDao(): DbService() {
//
//}


