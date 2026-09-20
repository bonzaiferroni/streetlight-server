package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import streetlight.model.data.Policy
import streetlight.server.db.tables.PolicyTable
import streetlight.server.db.tables.toPolicy
import streetlight.server.db.tables.createPolicy

class PolicyDao: DbService() {
    suspend fun create(policy: Policy) = dbQuery {
        PolicyTable.insert {
            it.createPolicy(policy)
        }
    }

    suspend fun readLabel(label: String) = dbQuery {
        PolicyTable.read { it.label.eq(label) }.firstOrNull()?.toPolicy()
    }
}