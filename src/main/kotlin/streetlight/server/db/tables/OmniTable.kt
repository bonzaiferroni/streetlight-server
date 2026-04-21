package streetlight.server.db.tables

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.OmniRecord
import java.util.UUID

object OmniTable: UUIDTable("omni") {
    val record = jsonb<OmniRecord>("record", tableJsonDefault)
    val recordAt = timestamp("record_at").index()
}

fun UpdateBuilder<*>.write(record: OmniRecord) {
    this[OmniTable.id] = UUID.randomUUID()
    this[OmniTable.record] = record
    this[OmniTable.recordAt] = record.recordAt
}