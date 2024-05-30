package streetlight.server.data.user

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import streetlight.model.User
import streetlight.server.data.getIdOrThrow
import streetlight.server.plugins.v1

fun Routing.userRouting(userService: UserService) {
    post("$v1/users") {
        val user = call.receive<User>()
        val id = userService.create(user)
        call.respond(HttpStatusCode.Created, id)
    }

    // Read user
    get("$v1/users/{id}") {
        val id = call.getIdOrThrow()
        val user = userService.read(id)
        if (user != null) {
            call.respond(HttpStatusCode.OK, user)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    // Update user
    put("$v1/users/{id}") {
        val id = call.getIdOrThrow()
        val user = call.receive<User>()
        userService.update(id, user)
        call.respond(HttpStatusCode.OK)
    }

    // Delete user
    delete("$v1/users/{id}") {
        val id = call.getIdOrThrow()
        userService.delete(id)
        call.respond(HttpStatusCode.OK)
    }
}
