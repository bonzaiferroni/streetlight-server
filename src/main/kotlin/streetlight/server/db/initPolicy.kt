package streetlight.server.db

import kampfire.utils.pascalToTitle
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import streetlight.model.data.Policy
import streetlight.model.data.PolicyId
import streetlight.model.data.SeedPolicy
import streetlight.server.db.services.PolicyDao
import kotlin.time.Clock

/** Creates each [SeedPolicy] not yet stored, matched by label. */
suspend fun initPolicy() = suspendTransaction {
    val dao = PolicyDao()
    val now = Clock.System.now()
    SeedPolicy.entries.forEach { value ->
        val label = value.name.pascalToTitle()
        if (dao.readLabel(label) != null) return@forEach
        val policy = Policy(
            policyId = PolicyId.random(),
            policyScope = value.policyScope,
            policyType = value.policyType,
            policyTarget = value.policyTarget,
            label = label,
            definition = value.definition,
            isDefault = value.isDefault,
            isReportable = value.isReportable,
            updatedAt = now,
            createdAt = now,
        )
        dao.create(policy)
    }
}