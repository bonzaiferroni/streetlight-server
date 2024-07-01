package streetlight.server.plugins

import io.ktor.server.application.*
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import streetlight.server.data.services.AreaTable
import streetlight.server.data.services.EventTable
import streetlight.server.data.services.RequestTable
import streetlight.server.data.services.LocationTable
import streetlight.server.data.services.PerformanceTable
import streetlight.server.data.services.UserService
import streetlight.server.data.services.UserTable
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
        SchemaUtils.create(PerformanceTable)
        SchemaUtils.create(RequestTable)
    }

    environment.monitor.subscribe(ApplicationStarted) {
        launch {
            val userCount = UserService().readAll().size
            if (userCount == 0) {
                println("backup restored")
                DbBackup.restore()
            } else {
                println("backup created")
                DbBackup.create()
            }
        }
    }
}
