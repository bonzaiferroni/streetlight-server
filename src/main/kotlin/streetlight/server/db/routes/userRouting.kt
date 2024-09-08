package streetlight.server.db.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.server.db.services.UserService
import streetlight.server.plugins.CLAIM_USERNAME
import streetlight.server.plugins.authenticateJwt
import streetlight.server.plugins.getClaim
import streetlight.server.plugins.v1

fun Routing.userRouting(service: UserService = UserService()) {

    authenticateJwt {
        get("$v1/user") {
            val username = call.getClaim(CLAIM_USERNAME)
            val userInfo = service.getUserInfo(username)
            call.respond(userInfo)
        }
    }
}