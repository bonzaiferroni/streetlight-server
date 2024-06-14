package streetlight.server.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import streetlight.server.data.area.AreaTable
import streetlight.server.data.event.EventTable
import streetlight.server.data.event.RequestTable
import streetlight.server.data.location.LocationTable
import streetlight.server.data.user.PerformanceTable
import streetlight.server.data.user.UserTable

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
}
