package streetlight.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.server.db.applyServiceRouting
import streetlight.server.db.core.authorize
import streetlight.server.db.services.AreaService
import streetlight.server.db.services.EventInfoService
import streetlight.server.db.services.EventService
import streetlight.server.db.services.RequestService
import streetlight.server.db.routes.eventInfoRouting
import streetlight.server.db.services.LocationService
import streetlight.server.db.services.RequestInfoService
import streetlight.server.db.routes.requestInfoRouting
import streetlight.server.db.services.SongService
import streetlight.server.db.routes.eventRouting

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

        eventInfoRouting(EventInfoService())
        requestInfoRouting(RequestInfoService())
        eventRouting(EventService())

        post("$v1/login") {
            call.authorize()
        }
    }
}

val v1 = "/api/v1"