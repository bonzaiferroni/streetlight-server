package streetlight.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.model.Api
import streetlight.model.apiPrefix
import streetlight.server.db.applyServiceRouting
import streetlight.server.db.core.authorize
import streetlight.server.db.routes.eventRouting
import streetlight.server.db.routes.requestInfoRouting
import streetlight.server.db.routes.userRouting
import streetlight.server.db.services.*

fun Application.configureApiRoutes() {
    routing {
        get(apiPrefix) {
            call.respondText("Hello World!")
        }

        applyServiceRouting(Api.area, AreaService())
        applyServiceRouting(Api.event, EventService())
        applyServiceRouting(Api.location, LocationService())
        applyServiceRouting(Api.request, RequestService())
        applyServiceRouting(Api.song, SongService())

        requestInfoRouting(RequestInfoService())
        eventRouting(EventService(), EventInfoService())
        userRouting(UserService())

        post(Api.login.path) {
            call.authorize()
        }
    }
}