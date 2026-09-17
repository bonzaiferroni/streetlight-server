package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import streetlight.server.model.ApiScope

fun ApiScope.serveSystem() {
    get("/health") { call.respond(HttpStatusCode.OK) }
}