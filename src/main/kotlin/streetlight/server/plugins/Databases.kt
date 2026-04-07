package streetlight.server.plugins

import io.ktor.server.application.*
import streetlight.server.db.initDb

fun Application.configureDatabases() {
    initDb()
}
