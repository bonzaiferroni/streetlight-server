package streetlight.server.db.services

import klutch.db.DbService
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import streetlight.model.data.Quorum
import streetlight.model.data.QuorumId
import streetlight.server.db.tables.QuorumTable
import streetlight.server.db.tables.writeFull

class QuorumTableDao(): DbService() {

    suspend fun create(quorum: Quorum) = dbQuery {
        QuorumTable.insertAndGetId {
            it.writeFull(quorum)
        }.value.let { QuorumId(it) }
    }
}