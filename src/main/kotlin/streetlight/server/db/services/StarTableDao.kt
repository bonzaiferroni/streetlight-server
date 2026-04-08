package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.tables.BasicUserTable
import klutch.utils.eqLowercase
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toStar

class StarTableDao: DbService() {

    suspend fun readByUsername(username: String) = dbQuery {
        BasicUserTable.leftJoin(StarTable).selectAll()
            .where { BasicUserTable.username.eqLowercase(username) }
            .map { it.toStar() }
            .firstOrNull()
    }


}