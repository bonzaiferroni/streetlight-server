package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.*
import streetlight.server.utils.toRecordId
import streetlight.server.utils.toRecordIdOrNull

object FlagTable: UuidTable("flag") {
    val flaggerId = reference("flagger_id", StarTable, ReferenceOption.CASCADE).index()
    val policyId = reference("policy_id", PolicyTable, ReferenceOption.CASCADE).index().nullable()
    val recordId = uuid("record_id").index()
    val recordType = enumeration<RecordType>("record_type")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toFlag() = Flag(
    flagId = toRecordId(FlagTable.id),
    flaggerId = toRecordId(FlagTable.flaggerId),
    policyId = toRecordIdOrNull(FlagTable.policyId),
    recordId = this[FlagTable.recordId],
    recordType = this[FlagTable.recordType],
    updatedAt = this[FlagTable.updatedAt],
    createdAt = this[FlagTable.createdAt],
)

fun UpdateBuilder<*>.writeFull(flag: Flag) {
    this[FlagTable.id] = flag.flagId.value
    this[FlagTable.createdAt] = flag.createdAt
    writeUpdate(flag)
}

fun UpdateBuilder<*>.writeUpdate(flag: Flag) {
    this[FlagTable.flaggerId] = flag.flaggerId.value
    this[FlagTable.policyId] = flag.policyId?.value
    this[FlagTable.recordId] = flag.recordId
    this[FlagTable.recordType] = flag.recordType
    this[FlagTable.updatedAt] = flag.updatedAt
}
