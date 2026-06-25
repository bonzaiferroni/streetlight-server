package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.UuidTable

object GalaxyPolicyTable: Table("galaxy_policy") {
    val policyId = reference("policy_id", PolicyTable, ReferenceOption.CASCADE).index()
    val galaxyId = reference("galaxy_id", GalaxyTable, ReferenceOption.CASCADE).index()

    override val primaryKey = PrimaryKey(policyId, galaxyId)
}