package streetlight.server.plugins

import io.ktor.server.application.*
import klutch.db.tables.RefreshTokenTable
import streetlight.server.db.initDb
import streetlight.server.db.tables.StarTable

fun Application.configureDatabases() {
    initDb(StarRefreshTokenTable)
}

val StarRefreshTokenTable = RefreshTokenTable(StarTable)
