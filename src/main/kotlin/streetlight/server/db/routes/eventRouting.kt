package streetlight.server.db.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.model.Api
import streetlight.model.core.Request
import streetlight.model.dto.ImageUploadRequest
import streetlight.server.db.services.EventInfoService
import streetlight.server.db.services.EventService
import streetlight.server.db.services.RequestInfoService
import streetlight.server.db.services.RequestService
import streetlight.server.extensions.getIdOrThrow
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun Routing.eventRouting(eventService: EventService, eventInfoService: EventInfoService) {
    get(Api.eventInfo.serverIdTemplate) {
        val id = call.getIdOrThrow()
        val event = eventInfoService.read(id)
        if (event != null) {
            call.respond(HttpStatusCode.OK, event)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get(Api.eventInfo.path) {
        val events = eventInfoService.readAll()
        call.respond(HttpStatusCode.OK, events)
    }

    get(Api.eventInfoCurrent.path) {
        val events = eventInfoService.readAllCurrent()
        call.respond(HttpStatusCode.OK, events)
    }

    // TODO: refactor song request creation
    post("$/event_profile/{id}/{song}") {
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

    post(Api.createEventRequest.path) {
        val request = call.receive<Request>()
        val id = RequestService().create(request.copy(time = System.currentTimeMillis()))
        call.respond(HttpStatusCode.Created, id)
    }

    get(Api.readEventRequests.serverIdTemplate) {
        val eventId = call.getIdOrThrow()
        val requests = RequestInfoService().readAllByEvent(eventId)
        call.respond(HttpStatusCode.OK, requests)
    }

    post(Api.uploadEventImage.path) {
        val request = call.receive<ImageUploadRequest>()
        var event = eventService.read(request.eventId) ?: return@post call.respond(HttpStatusCode.NotFound)
        val file = File("www/uploads/${request.filename}")
        file.parentFile.mkdirs()
        file.writeBytes(Base64.decode(request.image))
        event = event.copy(imageUrl = "static/uploads/${request.filename}")
        eventService.update(event)
        call.respond(HttpStatusCode.OK)
    }
}