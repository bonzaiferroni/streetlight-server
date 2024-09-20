package streetlight.server.plugins

import io.ktor.server.application.*
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import streetlight.server.db.services.UserService
import streetlight.server.db.tables.*
import streetlight.server.utilities.DbBackup

fun Application.configureDatabases() {
    val psqlPass = System.getenv("STREETLIGHT_PSQL_PW")
    val psqldb = Database.connect(
        "jdbc:pgsql://localhost:5432/streetlightdb",
        driver = "com.impossibl.postgres.jdbc.PGDriver",
        user = "streetlight",
        password = psqlPass
    )

    transaction(psqldb) {
        SchemaUtils.create(UserTable)
        SchemaUtils.create(LocationTable)
        SchemaUtils.create(AreaTable)
        SchemaUtils.create(EventTable)
        SchemaUtils.create(SongTable)
        SchemaUtils.create(RequestTable)
        SchemaUtils.create(SessionTokenTable)
    }

    environment.monitor.subscribe(ApplicationStarted) {
        launch {
            val userCount = UserService().readAll().size
            if (userCount == 0) {
                println("empty db found, restoring from backup")
                DbBackup.restore()
                println("backup restored")
            } else {
                println("creating backup")
                DbBackup.create()
                println("backup created")
            }
        }
    }
}
