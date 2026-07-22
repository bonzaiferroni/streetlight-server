package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import kabinet.console.globalConsole
import kampfire.model.Ok
import kampfire.model.outcomeOf
import kampfire.model.toOutcome
import streetlight.model.data.MapQuery
import klutch.server.*
import kotlinx.html.body
import kotlinx.html.p
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.server.model.*
import klutch.server.authGate

private val console = globalConsole.getHandle(ApiScope::serveEvents.name)

fun ApiScope.serveEvents() {
    val omni = provide<OmniService>()

    getApi(Api.Events) {
        Ok(dao.event.readActiveEvents())
    }

    get("/qr") {
        val event = dao.event.readActiveEvents().firstOrNull()
        if (event == null) {
            call.respondHtml(HttpStatusCode.NotFound) { body { p { +"Arr, no active events!" } } }
        } else {
            call.respondRedirect("/event-portal/${event.eventId.value}")
        }
    }

    authGate(optional = true) {
        getApi(Api.Events.ReadId, { it.toRecordId() }) {
            val identity = call.getIdentityOrNull()
            outcomeOf(dao.event.readEvent(it.data, identity?.callerId))
        }

        getApi(Api.Events.AtLocation) {
            val identity = call.getIdentityOrNull()
            Ok(dao.event.readLocationEvents(it.data, identity?.callerId))
        }

        getApi(Api.Events.QueryMap, MapQuery::fromQuery) {
            val sent = it.data
            val identity = call.getIdentityOrNull()
            Ok(dao.event.readEventsInBounds(sent.bounds, identity?.callerId))
        }

        postApi(Api.Events.ReadEventLocations) {
            val ids = it.data
            val identity = call.getIdentityOrNull()
            Ok(dao.event.readEventLocations(ids, identity?.callerId))
        }

        getApi(Api.Events.ReadSlug) {
            val slug = it.data
            val identity = call.getIdentityOrNull()
            outcomeOf(dao.event.readEventLocationBySlug(slug, identity?.callerId))
        }
    }

    authGate {
//        suspend fun <T> handleEdit(
//            edit: EventEdit,
//            identity: StarIdentity,
//            block: suspend (SavedImageSet?) -> T?
//        ): Response<T>? {
//            if (edit.eventId == null && dao.event.hasConflict(edit)) {
//                return Problem("Event already exists")
//            }
//
//            val imageUserId = identity.starId.takeIf { edit.imageRef?.isRelative ?: false }
//            val imageSet = saveImages(imageUserId, edit.eventId, edit.imageRef, EventTable.imageConfig)
//            return block(imageSet).toResponse()
//        }

        postApi(Api.Events.CreateEvent) { request ->
            val identity = call.getIdentity()
            val edit = request.data
            val title = requireNotNull(edit.title)

            createEvent(identity.callerId, edit)?.also { outcome ->
                if (outcome is Ok) {
                    val slug = outcome.data.slug
                    omni.sendEventCreated(title, slug, identity.username)
                }
            }
        }

        postApi(Api.Events.UpdateEvent) { request ->
            val identity = call.getIdentity()
            val edit = request.data
            val title = requireNotNull(edit.title)
            val eventId = requireNotNull(edit.eventId)

            updateEvent(eventId, identity.callerId, edit)?.also { outcome ->
                if (outcome is Ok) {
                    val slug = outcome.data.slug
                    omni.sendEventUpdated(title, slug, identity.username)
                }
            }
        }

        deleteApi(Api.Events.Delete) {
            val eventId = it.data
            val starId = call.getIdentity().callerId
            Ok(dao.event.deleteEvent(starId, eventId))
        }

        postApi(Api.Events.ParseSingleEvent) { request ->
            val request = request.data

            parseEvent(request)
        }

        postApi(Api.Events.ParseEvent) {
            error("not implemented")
        }

        getApi(Api.Events.ReadLights) {
            val callerId = call.getIdentity().callerId
            Ok(dao.light.readEventLights(callerId))
        }

        getApi(Api.Events.ReadUpdaterContent) {
            val slug = it.data
            readEventUpdaterContent(slug).toOutcome()
        }
    }
}
