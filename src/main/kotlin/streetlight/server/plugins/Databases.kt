package streetlight.server.plugins

import io.ktor.server.application.*
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import streetlight.server.db.services.*
import streetlight.server.utilities.DbBackup

fun Application.configureDatabases() {
    val database = Database.connect(
            url = "jdbc:h2:./test;DB_CLOSE_DELAY=-1",
            user = "root",
            driver = "org.h2.Driver",
            password = ""
        )

    transaction(database) {
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
