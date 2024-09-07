package streetlight.server.db.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import streetlight.model.Request
import streetlight.model.dto.ImageUploadRequest
import streetlight.server.db.getIdOrThrow
import streetlight.server.db.services.EventInfoService
import streetlight.server.db.services.EventService
import streetlight.server.db.services.RequestInfoService
import streetlight.server.db.services.RequestService
import streetlight.server.plugins.v1
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun Routing.eventRouting(eventService: EventService, eventInfoService: EventInfoService) {
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

    post("$v1/events/upload") {
        val request = call.receive<ImageUploadRequest>()
        var event = eventService.read(request.eventId) ?: return@post call.respond(HttpStatusCode.NotFound)
        val file = File("www/uploads/${request.filename}")
        file.parentFile.mkdirs()
        file.writeBytes(Base64.decode(request.image))
        event = event.copy(imageUrl = "static/uploads/${request.filename}")
        val result = eventService.update(request.eventId, event)
        if (result) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}