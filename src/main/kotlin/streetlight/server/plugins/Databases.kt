package streetlight.server.plugins

import streetlight.server.data.user.UserService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import streetlight.server.data.area.AreaService
import streetlight.server.data.area.areaRouting
import streetlight.server.data.event.EventService
import streetlight.server.data.event.eventRouting
import streetlight.server.data.location.LocationService
import streetlight.server.data.location.locationRouting
import streetlight.server.data.user.userRouting

fun Application.configureDatabases() {
    val database = Database.connect(
            url = "jdbc:h2:./test;DB_CLOSE_DELAY=-1",
            user = "root",
            driver = "org.h2.Driver",
            password = ""
        )
    val userService = UserService(database)
    val locationService = LocationService(database)
    val areaService = AreaService(database)
    val eventService = EventService(database)

    routing {
        userRouting(userService)
        locationRouting(locationService)
        areaRouting(areaService)
        eventRouting(eventService)
    }
}
