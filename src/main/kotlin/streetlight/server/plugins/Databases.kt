package streetlight.server.plugins

import streetlight.server.data.UserService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import streetlight.server.data.*

fun Application.configureDatabases() {
    val database = Database.connect(
            url = "jdbc:h2:./test;DB_CLOSE_DELAY=-1",
            user = "root",
            driver = "org.h2.Driver",
            password = ""
        )
    val userService = UserService(database)
    val locationService = LocationService(database)

    routing {
        userRouting(userService)
        locationRouting(locationService)
    }
}
