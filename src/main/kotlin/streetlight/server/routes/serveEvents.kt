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
import streetlight.model.data.EventEdited
import streetlight.model.data.Event
import streetlight.model.data.EventId
import streetlight.model.data.EventCreated
import streetlight.model.data.LightEdit
import streetlight.model.data.MultiLightEdit
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.EventTable
import streetlight.server.model.*
import kotlin.time.Clock

private val console = globalConsole.getHandle(StreetlightRouting::serveEvents.name)

fun StreetlightRouting.serveEvents() {
    val app = model
    val dao = app.dao.event
    val reader = EventParser(app)

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

    getEndpoint(Api.Events.AtLocation, { it.toProjectId()}) {
        dao.readLocationEvents(it.data)
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
            val identity = identity.getIdentity(call)
            val userId = identity.userId

            val edit = request.data

            if (edit.eventId == null && dao.hasConflict(request.data)) {
                call.respond(HttpStatusCode.Conflict)
                return@postEndpoint null
            }

            val imageUserId = userId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.eventId, edit.imageRef, EventTable.imageConfig)

            val eventId = edit.eventId
            if (eventId != null) {
                console.log("updating event: ${edit.title}")
                val event = dao.updateEvent(eventId, userId, edit, imageSet)
                app.service.omni.sendMessage(event.toEventEdited(identity.username))
                event
            } else {
                console.log("creating event: ${edit.title}")
                val event = dao.createEvent(userId, edit, imageSet)
                app.service.omni.sendMessage(event.toEventCreated(identity.username))
                event
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

//        postEndpoint(Api.Events.ParseMultiEvents) { request ->
//            val identity = identity.getUserIdentity(call)
//            if (!identity.isAdmin) return@postEndpoint null
//
//            reader.readEvents(request.data)
//        }

        postApi(Api.Events.ParseSingleEvent) { request ->
            val request = request.data

            reader.parseEvent(request)
        }

        postEndpoint(Api.Events.ParseEvent) {
            error("not implemented")
        }


        getEndpoint(Api.Events.ReadLights) {
            val starId = identity.getUserId(call)
            dao.readEventLights(starId)
        }

        postEndpoint(Api.Events.EditLight) {
            val starId = identity.getUserId(call)
            when (val request = it.data) {
                is LightEdit -> {
                    val isSuccess = dao.editEventLight(request, starId)
                    if (isSuccess && request.isLit) {
                        app.service.omni.sendBeacon(starId, request.stringId)
                    }
                    isSuccess
                }
                is MultiLightEdit -> {
                    dao.editEventLights(request.edits, starId)
                }
            }
        }
    }
}

private fun Event.toEventEdited(username: String) = EventEdited(eventId, title, username, Clock.System.now())
private fun Event.toEventCreated(username: String) = EventCreated(eventId, title, username, Clock.System.now())