package streetlight.server.db.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import streetlight.model.Request
import streetlight.server.db.getIdOrThrow
import streetlight.server.db.services.EventInfoService
import streetlight.server.db.services.RequestInfoService
import streetlight.server.db.services.RequestService
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

    get("$v1/event_info/current") {
        val events = eventInfoService.readAllCurrent()
        call.respond(HttpStatusCode.OK, events)
    }

    post("$v1/event_profile/{id}/{song}") {
        val eventId = call.getIdOrThrow()
        val songId = call.parameters["song"]?.toInt()
        if (songId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        val request = Request(
            id = 0,
            eventId = eventId,
            songId = songId,
            time = System.currentTimeMillis()
        )
        val id = RequestService().create(request)

        call.respond(HttpStatusCode.Created, id)
    }

    post("$v1/event_profile/request") {
        val request = call.receive<Request>()
        val id = RequestService().create(request.copy(time = System.currentTimeMillis()))
        call.respond(HttpStatusCode.Created, id)
    }

    get("$v1/event_profile/{id}") {
        val eventId = call.getIdOrThrow()
        val requests = RequestInfoService().readAllByEvent(eventId)
        call.respond(HttpStatusCode.OK, requests)
    }
}