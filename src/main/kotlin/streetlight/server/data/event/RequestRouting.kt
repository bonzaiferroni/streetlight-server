package streetlight.server.data.event

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import streetlight.model.Request
import streetlight.server.data.getIdOrThrow
import streetlight.server.plugins.v1

fun Routing.requestRouting(requestService: RequestService) {
    // Fetch all requests
    get("$v1/requests") {
        val requests = requestService.readAll()
        call.respond(HttpStatusCode.OK, requests)
    }

    // Read request
    get("$v1/requests/{id}") {
        val id = call.getIdOrThrow()
        val request = requestService.read(id)
        if (request != null) {
            call.respond(HttpStatusCode.OK, request)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    authenticate("auth-jwt") {
        post("$v1/requests") {
            val request = call.receive<Request>()
            val id = requestService.create(request)
            call.respond(HttpStatusCode.Created, id)
        }

        // Update request
        put("$v1/requests/{id}") {
            val id = call.getIdOrThrow()
            val request = call.receive<Request>()
            requestService.update(id, request)
            call.respond(HttpStatusCode.OK)
        }

        // Delete request
        delete("$v1/requests/{id}") {
            val id = call.getIdOrThrow()
            requestService.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}