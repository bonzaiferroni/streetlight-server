package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kabinet.console.globalConsole
import streetlight.model.data.MapQuery
import klutch.server.*
import klutch.utils.getUserId
import kotlinx.html.body
import kotlinx.html.p
import streetlight.model.Api
import streetlight.model.data.FileUse
import streetlight.model.data.toProjectId
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider

private val console = globalConsole.getHandle(Routing::serveEvents.name)

fun Routing.serveEvents(app: ServerProvider = RuntimeProvider) {
    val dao = app.dao.event
    val reader = EventUrlReader(app)

    getEndpoint(Api.Events) {
        dao.readActiveEvents()
    }

    getEndpoint(Api.EventProfile, { it.toProjectId() }) { id, _ ->
        dao.readEvent(id)
    }

    queryEndpoint(Api.Events.QueryMap, MapQuery::fromQuery) { sent, endpoint ->
        if (sent == null) return@queryEndpoint emptyList()
        app.dao.event.readEventsInBounds(sent.bounds)
    }

    get("/qr") {
        val event = dao.readActiveEvents().firstOrNull()
        if (event == null) {
            call.respondHtml(HttpStatusCode.NotFound) { body { p { +"Arr, no active events!" } } }
        } else {
            call.respondRedirect("/event-portal/${event.eventId.value}")
        }
    }

    getEndpoint(Api.Events.AtLocation, { it.toProjectId()}) { id, _ ->
        dao.readLocationEvents(id)
    }

    authenticateJwt {
        postEndpoint(Api.Events.Edit) { request ->
            val userId = getUserId()
            val locationId = dao.findOrCreateLocation(userId, request.data)
            if (locationId == null) {
                call.respond(HttpStatusCode.BadRequest, "Unable to create location")
                return@postEndpoint null
            }

            var edit = request.data.copy(
                locationId = locationId
            )
            if (dao.hasConflict(request.data)) {
                call.respond(HttpStatusCode.Conflict)
                return@postEndpoint null
            }

            edit = edit.let { edit ->
                val imageUrl = downloadExternalImage(edit.imageUrl)
                val thumbUrl = createThumb(imageUrl, edit.thumbUrl)
                edit.copy(imageUrl = imageUrl, thumbUrl = thumbUrl)
            }

            val eventId = edit.eventId
            if (eventId != null) {
                console.log("updating event: ${edit.title}")
                dao.updateEvent(eventId, userId, edit)
            } else {
                console.log("creating event: ${edit.title}")
                dao.createEvent(userId, edit)
            }
        }

        updateEndpoint(Api.EventProfile.Update) { update, _ ->
            val userId = getUserId()
            dao.updateEvent(userId, update)
        }

        deleteEndpoint(Api.Events.Delete) { eventId, _ ->
            val userId = getUserId()
            dao.deleteEvent(userId, eventId)
        }

//        webSocket(Api.Events.UserEvents.path) {
//            val userId = call.getUserId()
//
//        }
        postEndpoint(Api.Events.Upload) { bytes, _ ->
            val userId = getUserId()
            val format = validateImage(bytes) ?: return@postEndpoint null
            uploadImageFile(bytes, userId, FileUse.FullImage, format)
        }

        postEndpoint(Api.Events.ParseEvents) { request ->
            reader.serve(request.data)
        }
    }
}

