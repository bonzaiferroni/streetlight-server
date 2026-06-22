package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.core.ResultRow
import streetlight.model.data.Quorum
import streetlight.model.data.QuorumId
import streetlight.model.data.RecordType
import streetlight.model.data.Question

object QuorumTable : UuidTable("quorum") {
    val recordId = uuid("record_id").index()
    val recordType = enumeration<RecordType>("record_type")
    val question = enumeration<Question>("question")
    val decision = integer("decision").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toQuorum() = Quorum(
    quorumId = QuorumId(this[QuorumTable.id].value),
    recordId = this[QuorumTable.recordId],
    recordType = this[QuorumTable.recordType],
    question = this[QuorumTable.question],
    decision = this[QuorumTable.decision],
    updatedAt = this[QuorumTable.updatedAt],
    createdAt = this[QuorumTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(quorum: Quorum) {
    this[QuorumTable.id] = quorum.quorumId.value
    this[QuorumTable.createdAt] = quorum.createdAt
    writeUpdate(quorum)
}

fun UpdateBuilder<*>.writeUpdate(quorum: Quorum) {
    this[QuorumTable.recordId] = quorum.recordId
    this[QuorumTable.recordType] = quorum.recordType
    this[QuorumTable.question] = quorum.question
    this[QuorumTable.decision] = quorum.decision
    this[QuorumTable.updatedAt] = quorum.updatedAt
}
