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
import streetlight.model.ArtEvent
import streetlight.server.data.getIdOrThrow
import streetlight.server.plugins.v1

fun Routing.artEventRouting(artEventService: ArtEventService) {
    get("$v1/art_events") {
        val artEvents = artEventService.readAll()
        call.respond(artEvents)
    }

    // Read artEvent
    get("$v1/art_events/{id}") {
        val id = call.getIdOrThrow()
        val artEvent = artEventService.read(id)
        if (artEvent != null) {
            call.respond(HttpStatusCode.OK, artEvent)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    authenticate("auth-jwt") {
        post("$v1/art_events") {
            val artEvent = call.receive<ArtEvent>()
            val id = artEventService.create(artEvent)
            call.respond(HttpStatusCode.Created, id)
        }

        // Update artEvent
        put("$v1/art_events/{id}") {
            val id = call.getIdOrThrow()
            val artEvent = call.receive<ArtEvent>()
            artEventService.update(id, artEvent)
            call.respond(HttpStatusCode.OK)
        }

        // Delete artEvent
        delete("$v1/art_events/{id}") {
            val id = call.getIdOrThrow()
            artEventService.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}