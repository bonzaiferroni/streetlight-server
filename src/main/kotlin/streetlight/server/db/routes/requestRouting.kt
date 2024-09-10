package streetlight.server.db.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.model.Api
import streetlight.server.db.services.RequestInfoService
import streetlight.server.extensions.getIdOrThrow

fun Routing.requestInfoRouting(requestInfoService: RequestInfoService) {
    get(Api.requestInfo.serverIdTemplate) {
        val id = call.getIdOrThrow()
        val request = requestInfoService.read(id)
        if (request != null) {
            call.respond(HttpStatusCode.OK, request)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get(Api.requestInfo.path) {
        val requests = requestInfoService.readAll()
        call.respond(HttpStatusCode.OK, requests)
    }

    get(Api.requestInfo.serverIdTemplate) {
        val eventId = call.getIdOrThrow()
        val requests = requestInfoService.readAllByEvent(eventId).filter { !it.performed }
        call.respond(HttpStatusCode.OK, requests)
    }

    get(Api.requestInfoQueue.serverIdTemplate) {
        val eventId = call.getIdOrThrow()
        val requests = requestInfoService.getQueue(eventId)
        call.respond(HttpStatusCode.OK, requests)
    }

    get(Api.requestInfoRandom.serverIdTemplate) {
        val eventId = call.getIdOrThrow()
        val request = requestInfoService.getRandomRequest(eventId)
        if (request != null) {
            call.respond(HttpStatusCode.OK, request)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}