package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.*
import streetlight.server.utils.toRecordId

object PolicyTable: UuidTable("policy") {
    val policyScope = enumeration<PolicyScope>("policy_scope")
    val policyType = enumeration<PolicyType>("policy_type")
    val policyTarget = enumeration<PolicyTarget>("policy_target")
    val label = text("label")
    val definition = text("definition")
    val isDefault = bool("is_default")
    val isReportable = bool("is_reportable")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toPolicy() = Policy(
    policyId = toRecordId(PolicyTable.id),
    policyScope = this[PolicyTable.policyScope],
    policyType = this[PolicyTable.policyType],
    policyTarget = this[PolicyTable.policyTarget],
    label = this[PolicyTable.label],
    definition = this[PolicyTable.definition],
    isDefault = this[PolicyTable.isDefault],
    isReportable = this[PolicyTable.isReportable],
    updatedAt = this[PolicyTable.updatedAt],
    createdAt = this[PolicyTable.createdAt],
)

fun UpdateBuilder<*>.createRecord(policy: Policy) {
    this[PolicyTable.id] = policy.policyId.value
    this[PolicyTable.createdAt] = policy.createdAt
    this[PolicyTable.isDefault] = policy.isDefault
    writeUpdate(policy)
}

fun UpdateBuilder<*>.writeUpdate(policy: Policy) {
    this[PolicyTable.policyScope] = policy.policyScope
    this[PolicyTable.policyType] = policy.policyType
    this[PolicyTable.policyTarget] = policy.policyTarget
    this[PolicyTable.label] = policy.label
    this[PolicyTable.definition] = policy.definition
    this[PolicyTable.isReportable] = policy.isReportable
    this[PolicyTable.updatedAt] = policy.updatedAt
}