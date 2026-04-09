package streetlight.server.db.services

import klutch.db.DbService
import klutch.utils.eqLowercase
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toStar

class StarTableDao: DbService() {

    suspend fun readByUsername(username: String) = dbQuery {
        StarTable.selectAll()
            .where { StarTable.username.eqLowercase(username) }
            .map { it.toStar() }
            .firstOrNull()
    }


}