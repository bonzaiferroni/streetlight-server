package streetlight.server.db.services

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import streetlight.server.db.getIdOrThrow
import streetlight.server.plugins.v1

fun Routing.requestInfoRouting(requestInfoService: RequestInfoService) {
    get("$v1/request_info/{id}") {
        val id = call.getIdOrThrow()
        val request = requestInfoService.read(id)
        if (request != null) {
            call.respond(HttpStatusCode.OK, request)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("$v1/request_info") {
        val requests = requestInfoService.readAll()
        call.respond(HttpStatusCode.OK, requests)
    }

    get("$v1/request_info/event/{id}") {
        val eventId = call.getIdOrThrow()
        val requests = requestInfoService.readAllByEvent(eventId).filter { !it.performed }
        call.respond(HttpStatusCode.OK, requests)
    }

    get("$v1/request_info/{id}/queue") {
        val eventId = call.getIdOrThrow()
        var requests = requestInfoService.getQueue(eventId)
        if (requests.isEmpty()) {
            val random = requestInfoService.getRandomRequest(eventId)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            requests = listOf(random)
        }
        call.respond(HttpStatusCode.OK, requests)
    }
}