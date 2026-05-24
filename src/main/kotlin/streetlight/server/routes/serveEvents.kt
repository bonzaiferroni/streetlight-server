package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import kabinet.console.globalConsole
import kampfire.api.Slug
import kampfire.model.ApiResponse
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.responseOf
import kampfire.model.toResponse
import streetlight.model.data.MapQuery
import klutch.server.*
import kotlinx.html.body
import kotlinx.html.p
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.server.db.tables.EventTable
import streetlight.server.model.*
import klutch.server.authGate
import streetlight.model.data.EventEdit
import streetlight.server.db.tables.SavedImageSet

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

    getApi(Api.Events.AtLocation, { it.toRecordId()}) {
        Ok(dao.event.readLocationEvents(it.data))
    }

    postApi(Api.Events.ReadEventLocations) {
        val ids = it.data
        Ok(dao.event.readEventLocations(ids))
    }

    getApi(Api.Events.ReadSlug) {
        val slug = it.data
        responseOf(dao.event.readEventLocationBySlug(slug))
    }

    getApi(Api.Events.ReadId, { it.toRecordId() }) {
        responseOf(dao.event.readEvent(it.data))
    }

    authGate {
        suspend fun handleEdit(
            edit: EventEdit,
            identity: StarIdentity,
            block: suspend (SavedImageSet) -> Slug?
        ): ApiResponse<Slug>? {
            if (edit.eventId == null && dao.event.hasConflict(edit)) {
                return Problem("Event already exists")
            }

            val imageUserId = identity.starId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.eventId, edit.imageRef, EventTable.imageConfig)
            return block(requireNotNull(imageSet)).toResponse()
        }

        postApi(Api.Events.CreateEvent) { request ->
            val identity = call.getIdentity()
            val edit = request.data
            val title = requireNotNull(edit.title)

            handleEdit(edit, identity) { imageSet ->
                console.log("creating event: $title")
                dao.event.createEvent(identity.starId, edit, imageSet).also { slug ->
                    omni.sendEventCreated(title, slug, identity.username)
                }
            }
        }

        postApi(Api.Events.UpdateEvent) { request ->
            val identity = call.getIdentity()
            val edit = request.data
            val title = requireNotNull(edit.title)

            handleEdit(edit, identity) { imageSet ->
                val eventId = requireNotNull(edit.eventId)
                console.log("updating event: ${edit.title}")
                dao.event.updateEvent(eventId, identity.starId, edit, imageSet).also { slug ->
                    omni.sendEventUpdated(title, slug, identity.username)
                }
            }
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
