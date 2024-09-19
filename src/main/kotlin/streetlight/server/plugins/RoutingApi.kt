package streetlight.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.model.Api
import streetlight.model.apiPrefix
import streetlight.server.db.core.authorize
import streetlight.server.db.routes.dataRouting

fun Application.configureApiRoutes() {
    routing {
        get(apiPrefix) {
            call.respondText("Hello World!")
        }

        dataRouting()

        post(Api.login.path) {
            call.authorize()
        }
    }
}