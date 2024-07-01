package streetlight.server.data.services

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import streetlight.model.Request
import streetlight.server.data.getIdOrThrow
import streetlight.server.plugins.v1

fun Routing.eventInfoRouting(eventInfoService: EventInfoService) {
    get("$v1/event_info/{id}") {
        val id = call.getIdOrThrow()
        val event = eventInfoService.read(id)
        if (event != null) {
            call.respond(HttpStatusCode.OK, event)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("$v1/event_info") {
        val events = eventInfoService.readAll()
        call.respond(HttpStatusCode.OK, events)
    }

    post("$v1/event_profile/{id}/{perf}") {
        val eventId = call.getIdOrThrow()
        val performanceId = call.parameters["perf"]?.toInt()
        if (performanceId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        val request = Request(
            id = 0,
            eventId = eventId,
            performanceId = performanceId,
            time = System.currentTimeMillis()
        )
        val id = RequestService().create(request)

        call.respond(HttpStatusCode.Created, id)
    }

    get("$v1/event_profile/{id}") {
        val eventId = call.getIdOrThrow()
        val requests = RequestInfoService().readAllByEvent(eventId)
        call.respond(HttpStatusCode.OK, requests)
    }
}