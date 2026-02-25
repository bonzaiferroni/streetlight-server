package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
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
import streetlight.server.routes.uploadImageFile

private val console = globalConsole.getHandle(Routing::serveEvents.name)

fun Routing.serveEvents(app: ServerProvider = RuntimeProvider) {
    val dao = app.dao.event
    val agent = UrlParser(app.env.read("GEMINI_KEY_A"))

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

    authenticateJwt {
        postEndpoint(Api.Events.Edit) { request ->
            val userId = getUserId()
            val edit = request.body.let { edit ->
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

        postEndpoint(Api.Events.ReadUrl) {
            val url = it.body.url.takeIf { url -> url.isNotEmpty() } ?: return@postEndpoint null
            if (it.body.isImage) {
                agent.readImage(url, eventInstructions)
            } else {
                agent.readHtml(url, eventInstructions)
            }
        }
    }
}

val eventInstructions = """
        Read the following html. We believe it is information about an event or a list of events.
        For each event, your job is to extract the following json properties, if that information can be found in the content:
        
        * name: Event name or title 
        * time: Time of day of the event as 24-hour value [HH:MM]
        * date: Date of the event as ISO local date [YYYY-MM-DD]
        * location: The name or description of the location of the event
        * address: The address at which the event is located
        * imageUrl: The featured image for the event, must be a full url
        * description: Additional details given about the event
        * ageMin: The minimum age for attendees
        * contact: Any name and/or contact information given for the event
        * url: Url for more information about the event, must be a full url
""".trimIndent()

