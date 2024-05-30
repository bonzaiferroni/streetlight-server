package streetlight.server.data.event

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import streetlight.model.Event
import streetlight.server.data.getIdOrThrow
import streetlight.server.plugins.v1

fun Routing.eventRouting(eventService: EventService) {

    // Fetch all events
    get("$v1/events") {
        val events = eventService.readAll()
        call.respond(HttpStatusCode.OK, events)
    }

    // Read event
    get("$v1/events/{id}") {
        val id = call.getIdOrThrow()
        val event = eventService.read(id)
        if (event != null) {
            call.respond(HttpStatusCode.OK, event)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    authenticate("auth-jwt") {
        post("$v1/events") {
            val event = call.receive<Event>()
            val id = eventService.create(event)
            call.respond(HttpStatusCode.Created, id)
        }

        // Update event
        put("$v1/events/{id}") {
            val id = call.getIdOrThrow()
            val event = call.receive<Event>()
            eventService.update(id, event)
            call.respond(HttpStatusCode.OK)
        }

        // Delete event
        delete("$v1/events/{id}") {
            val id = call.getIdOrThrow()
            eventService.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}