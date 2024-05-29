package streetlight.server.plugins

import streetlight.server.data.user.UserService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import streetlight.server.data.area.AreaService
import streetlight.server.data.area.AreaTable
import streetlight.server.data.area.areaRouting
import streetlight.server.data.event.EventService
import streetlight.server.data.event.EventTable
import streetlight.server.data.event.eventRouting
import streetlight.server.data.location.LocationService
import streetlight.server.data.location.LocationTable
import streetlight.server.data.location.locationRouting
import streetlight.server.data.user.UserTable
import streetlight.server.data.user.userRouting

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
    }

    routing {
        userRouting(UserService())
        locationRouting(LocationService())
        areaRouting(AreaService())
        eventRouting(EventService())
    }
}
