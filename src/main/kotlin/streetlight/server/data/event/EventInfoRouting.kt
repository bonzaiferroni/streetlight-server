package streetlight.server.data.event

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
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
}