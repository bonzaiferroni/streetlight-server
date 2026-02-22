package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.request.contentType
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
import streetlight.agent.UrlParser
import streetlight.model.Api
import streetlight.model.data.EventInfo
import streetlight.model.data.FileUse
import streetlight.model.data.toProjectId
import streetlight.model.mockDb
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider
import java.io.File

private val console = globalConsole.getHandle(Routing::serveEvents.name)

fun Routing.serveEvents(app: ServerProvider = RuntimeProvider) {
    val dao = app.dao.event
    val agent = UrlParser(app.env.read("GEMINI_KEY_B"))

    getEndpoint(Api.Events) {
        dao.readActiveEvents()
    }

    getEndpoint(Api.EventProfile, { it.toProjectId() }) { id, _ ->
        dao.readEvent(id)
    }

    queryEndpoint(Api.Events.QueryMap, MapQuery::fromQuery) { sent, endpoint ->
        if (sent == null) return@queryEndpoint emptyList()
        val locations = mockDb.locations.filter { sent.bounds.contains(it.geoPoint) }
        val locationIds = locations.map { it.locationId }.toSet()
        val events = mockDb.events.filter { locationIds.contains(it.locationId) }
        events.map { event -> EventInfo.from(event, locations.first { it.locationId == event.locationId }) }
    }

    get("/qr") {
        val event = dao.readActiveEvents().firstOrNull()
        if (event == null) {
            call.respondHtml(HttpStatusCode.NotFound) { body { p { +"Arr, no active events!" } } }
        } else {
            call.respondRedirect("/event-portal/${event.eventId.value}")
        }
    }

    postEndpoint(Api.Events.ReadUrl) {
        val url = it.body.removeSurrounding("\"").takeIf { url -> url.startsWith("http") } ?: return@postEndpoint null
        agent.read(url, eventInstructions)
    }

    authenticateJwt {
        postEndpoint(Api.Events.Create) { newEvent, _ ->
            val userId = getUserId()
            dao.createEvent(userId, newEvent)
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
            uploadUserImage(bytes, userId, FileUse.EventImage)
        }
    }
}

