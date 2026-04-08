package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import kabinet.console.globalConsole
import kampfire.model.ImageSize
import streetlight.model.data.MapQuery
import klutch.server.*
import klutch.utils.getUserId
import kotlinx.html.body
import kotlinx.html.p
import streetlight.model.Api
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.deleteCurrentImages
import streetlight.server.db.tables.saveImages
import streetlight.server.model.*

private val console = globalConsole.getHandle(RoutingContext<Streetlight>::serveEvents.name)

fun StreetlightRouting.serveEvents() {
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

    postEndpoint(Api.Events.ReadEventLocations) {
        val ids = it.data
        dao.readEventLocations(ids)
    }

    getEndpoint(Api.Events.ReadBySlug) {
        val slug = it.data
        dao.readEventBySlug(slug)
    }

    authenticateJwt {
        postEndpoint(Api.Events.Edit) { request ->
            val userId = getUserId()

            var edit = request.data

            if (dao.hasConflict(request.data)) {
                call.respond(HttpStatusCode.Conflict)
                return@postEndpoint null
            }

            deleteCurrentImages(edit.eventId, edit.imageUrl, EventTable.imageConfig)
            val imageValues = saveImages(edit.imageUrl, EventTable.imageConfig)

            val eventId = edit.eventId
            if (eventId != null) {
                console.log("updating event: ${edit.title}")
                dao.updateEvent(eventId, userId, edit, imageValues)
            } else {
                console.log("creating event: ${edit.title}")
                dao.createEvent(userId, edit, imageValues)
            }
        }

        deleteEndpoint(Api.Events.Delete) { eventId, _ ->
            val userId = getUserId()
            dao.deleteEvent(userId, eventId)
        }

//        webSocket(Api.Events.UserEvents.path) {
//            val userId = call.getUserId()
//
//        }

        postEndpoint(Api.Events.ParseMultiEvents) { request ->
            reader.serveMulti(request.data)
        }

        postEndpoint(Api.Events.ParseSingleEvent) { request ->
            reader.serveSingle(request.data)
        }

        getEndpoint(Api.Events.ReadLights) {
            val userId = getUserId()
            dao.readEventLights(userId)
        }

        postEndpoint(Api.Events.EditLight) {
            val userId = getUserId()
            dao.editEventLight(it.data, userId)
        }
    }
}

