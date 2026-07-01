package streetlight.server.plugins

import io.ktor.server.application.*
import klutch.db.tables.RefreshTokenTable
import streetlight.model.data.StarId
import streetlight.server.db.initDb
import streetlight.server.db.tables.StarTable
import streetlight.server.model.ServerScope

fun Application.configureDatabases(server: ServerScope) {
    initDb(server)
}

