package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import kabinet.console.globalConsole
import streetlight.model.data.MapQuery
import klutch.server.*
import kotlinx.html.body
import kotlinx.html.p
import streetlight.model.Api
import streetlight.model.data.EventId
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.EventTable
import streetlight.server.model.*

private val console = globalConsole.getHandle(StreetlightRouting::serveEvents.name)

fun StreetlightRouting.serveEvents() {
    val dao = app.dao.event
    val reader = EventUrlReader(app)

    getEndpoint(Api.Events) {
        dao.readActiveEvents()
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

    getEndpoint(Api.Events.ReadEventLocationBySlug) {
        val slug = it.data
        dao.readEventLocationBySlug(slug)
    }

    getEndpoint(Api.Events.ReadById, { EventId(it) }) {
        dao.readEvent(it.data)
    }

    authenticateJwt {
        postEndpoint(Api.Events.Edit) { request ->
            val userId = identity.getUserId(call)

            val edit = request.data

            if (dao.hasConflict(request.data)) {
                call.respond(HttpStatusCode.Conflict)
                return@postEndpoint null
            }

            val imageUserId = userId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.eventId, edit.imageRef, EventTable.imageConfig)

            val eventId = edit.eventId
            if (eventId != null) {
                console.log("updating event: ${edit.title}")
                dao.updateEvent(eventId, userId, edit, imageSet)
            } else {
                console.log("creating event: ${edit.title}")
                dao.createEvent(userId, edit, imageSet)
            }
        }

        deleteEndpoint(Api.Events.Delete) { eventId, _ ->
            val starId = identity.getUserId(call)
            dao.deleteEvent(starId, eventId)
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
            val starId = identity.getUserId(call)
            dao.readEventLights(starId)
        }

        postEndpoint(Api.Events.EditLight) {
            val starId = identity.getUserId(call)
            dao.editEventLight(it.data, starId)
        }
    }
}

