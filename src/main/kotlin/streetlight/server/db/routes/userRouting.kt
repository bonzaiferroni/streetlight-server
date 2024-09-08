package streetlight.server.db.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.model.dto.SignUpInfo
import streetlight.server.db.services.UserService
import streetlight.server.plugins.CLAIM_USERNAME
import streetlight.server.plugins.authenticateJwt
import streetlight.server.plugins.getClaim
import streetlight.server.plugins.v1

fun Routing.userRouting(service: UserService = UserService()) {

    post("$v1/user") {
        val info = call.receive<SignUpInfo>()
        try {
            service.createUser(info)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid data")
            return@post
        }
        call.respond(status = HttpStatusCode.OK, "User created")
    }

    authenticateJwt {
        get("$v1/user") {
            val username = call.getClaim(CLAIM_USERNAME)
            val userInfo = service.getUserInfo(username)
            call.respond(userInfo)
        }
    }
}