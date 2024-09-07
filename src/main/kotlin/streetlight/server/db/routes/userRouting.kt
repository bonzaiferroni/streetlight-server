package streetlight.server.db.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.server.db.services.UserService
import streetlight.server.db.tables.UserTable.username
import streetlight.server.plugins.v1

fun Routing.userRouting(service: UserService = UserService()) {
    post("$v1/users") {
        var username = call.receiveText()
        println(username)
        if (username.length < 3 || !username.startsWith('"') || !username.endsWith('"')) {
            call.respondText("Invalid username", status = HttpStatusCode.BadRequest)
            return@post
        }
        username = username.substring(1, username.length - 1)
        val userInfo = service.getUserInfo(username)
        call.respond(userInfo)
    }
}