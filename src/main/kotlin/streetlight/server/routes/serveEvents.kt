package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import kabinet.console.globalConsole
import kampfire.model.Ok
import kampfire.model.responseOf
import streetlight.model.data.MapQuery
import klutch.server.*
import kotlinx.html.body
import kotlinx.html.p
import streetlight.model.Api
import streetlight.model.data.EventEdited
import streetlight.model.data.Event
import streetlight.model.data.EventCreated
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.EventTable
import streetlight.server.model.*
import klutch.server.authGate
import kotlin.time.Clock

private val console = globalConsole.getHandle(ApiContext::serveEvents.name)

fun ApiContext.serveEvents() {
    val reader = server.get<EventParser>()
    val omni = server.get<OmniService>()

    getApi(Api.Events) {
        Ok(dao.event.readActiveEvents())
    }

    getApi(Api.Events.QueryMap, MapQuery::fromQuery) {
        val sent = it.data
        Ok(dao.event.readEventsInBounds(sent.bounds))
    }

    get("/qr") {
        val event = dao.event.readActiveEvents().firstOrNull()
        if (event == null) {
            call.respondHtml(HttpStatusCode.NotFound) { body { p { +"Arr, no active events!" } } }
        } else {
            call.respondRedirect("/event-portal/${event.eventId.value}")
        }
    }

    getApi(Api.Events.AtLocation, { it.toProjectId()}) {
        Ok(dao.event.readLocationEvents(it.data))
    }

    postApi(Api.Events.ReadEventLocations) {
        val ids = it.data
        Ok(dao.event.readEventLocations(ids))
    }

    getApi(Api.Events.ReadBySlug) {
        val slug = it.data
        responseOf(dao.event.readEventBySlug(slug))
    }

    getApi(Api.Events.ReadEventLocationBySlug) {
        val slug = it.data
        responseOf(dao.event.readEventLocationBySlug(slug))
    }

    getApi(Api.Events.ReadById, { it.toProjectId() }) {
        responseOf(dao.event.readEvent(it.data))
    }

    authGate {
        postApi(Api.Events.Edit) { request ->
            val identity = call.getIdentity()
            val userId = identity.starId

            val edit = request.data

            if (edit.eventId == null && dao.event.hasConflict(request.data)) {
                call.respond(HttpStatusCode.Conflict)
                return@postApi null
            }

            val imageUserId = userId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.eventId, edit.imageRef, EventTable.imageConfig)

            val eventId = edit.eventId
            val event = if (eventId != null) {
                console.log("updating event: ${edit.title}")
                val event = dao.event.updateEvent(eventId, userId, edit, imageSet)
                omni.sendMessage(event.toEventEdited(identity.username))
                event
            } else {
                console.log("creating event: ${edit.title}")
                val event = dao.event.createEvent(userId, edit, imageSet)
                omni.sendMessage(event.toEventCreated(identity.username))
                event
            }
            responseOf(event)
        }

        deleteApi(Api.Events.Delete) {
            val eventId = it.data
            val starId = call.getIdentity().starId
            Ok(dao.event.deleteEvent(starId, eventId))
        }

        postApi(Api.Events.ParseSingleEvent) { request ->
            val request = request.data

            reader.parseEvent(request)
        }

        postApi(Api.Events.ParseEvent) {
            error("not implemented")
        }


        getApi(Api.Events.ReadLights) {
            val starId = call.getIdentity().starId
            Ok(dao.light.readEventLights(starId))
        }
    }
}

private fun Event.toEventEdited(username: String) = EventEdited(eventId, title, username, Clock.System.now())
private fun Event.toEventCreated(username: String) = EventCreated(eventId, title, username, Clock.System.now())