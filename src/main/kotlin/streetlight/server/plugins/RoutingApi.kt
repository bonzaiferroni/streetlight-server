package streetlight.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.server.db.applyServiceRouting
import streetlight.server.db.core.authorize
import streetlight.server.db.routes.requestInfoRouting
import streetlight.server.db.routes.eventRouting
import streetlight.server.db.routes.userRouting
import streetlight.server.db.services.*

fun Application.configureApiRoutes() {
    routing {
        get(v1) {
            call.respondText("Hello World!")
        }

        applyServiceRouting("areas", AreaService())
        applyServiceRouting("events", EventService())
        applyServiceRouting("locations", LocationService())
        applyServiceRouting("requests", RequestService())
        applyServiceRouting("songs", SongService())

        requestInfoRouting(RequestInfoService())
        eventRouting(EventService(), EventInfoService())
        userRouting(UserService())

        post("$v1/login") {
            call.authorize()
        }
    }
}

val v1 = "/api/v1"