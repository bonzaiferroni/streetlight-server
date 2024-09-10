package streetlight.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.model.Api
import streetlight.model.apiPrefix
import streetlight.server.db.dataRouting
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

        dataRouting(Api.area, AreaService())
        dataRouting(Api.event, EventService())
        dataRouting(Api.location, LocationService())
        dataRouting(Api.request, RequestService())
        dataRouting(Api.song, SongService())

        requestInfoRouting(RequestInfoService())
        eventRouting(EventService(), EventInfoService())
        userRouting(UserService())

        post(Api.login.path) {
            call.authorize()
        }
    }
}