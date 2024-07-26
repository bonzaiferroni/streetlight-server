package streetlight.server.db.services

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import streetlight.model.dto.ImageUploadRequest
import streetlight.server.plugins.v1
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun Routing.eventRouting(eventService: EventService) {
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