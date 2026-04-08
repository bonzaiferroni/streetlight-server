package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.tables.UserTable
import klutch.utils.eqLowercase
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toStar

class StarTableDao: DbService() {

    suspend fun readByUsername(username: String) = dbQuery {
        UserTable.leftJoin(StarTable).selectAll()
            .where { UserTable.username.eqLowercase(username) }
            .map { it.toStar() }
            .firstOrNull()
    }


}