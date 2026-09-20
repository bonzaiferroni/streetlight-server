package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.model.CallerId
import org.jetbrains.exposed.v1.jdbc.insert
import streetlight.model.data.BugEdit
import streetlight.server.db.tables.BugTable
import streetlight.server.db.tables.createBug

class BugTableDao: DbService() {

    suspend fun create(edit: BugEdit, callerId: CallerId?, buildId: String) = dbQuery {
        BugTable.insert { it.createBug(edit, callerId, buildId) }.insertedCount == 1
    }
}
