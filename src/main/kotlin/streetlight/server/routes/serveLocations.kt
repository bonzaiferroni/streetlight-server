package streetlight.server.routes

import io.ktor.server.routing.Routing
import kabinet.console.globalConsole
import kampfire.model.GeoPoint
import kampfire.model.kilometers
import klutch.server.*
import klutch.utils.getUserId
import streetlight.agent.UrlParser
import streetlight.model.Api
import streetlight.model.data.toProjectId
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider

private val console = globalConsole.getHandle(Routing::serveLocations.name)

fun Routing.serveLocations(app: ServerProvider = RuntimeProvider) {
    val dao = app.dao.location
    val agent = UrlParser(app.env.read("GEMINI_KEY_A"))

    getEndpoint(Api.Locations.Street, { it.toProjectId() }) { id, _ ->
        dao.readLocations(id)
    }

    getEndpoint(Api.Locations, { it.toProjectId() }) { id, _ ->
        dao.readLocation(id)
    }

    getEndpoint(Api.Locations.Search) { endpoint ->
        val query = readParam(endpoint.query)
        dao.searchLocations(query)
    }

    getEndpoint(Api.Locations.ReadTop) { endpoint ->
        val count = readParam(endpoint.count)
        dao.readTop(count)
    }

    queryEndpoint(Api.Locations.QueryPoint, GeoPoint::fromQuery) { sent, endpoint ->
        sent?.let {
            dao.readNearbyLocations(sent, 1.kilometers)
        }
    }

    authenticateJwt {

        postEndpoint(Api.Locations.Create) { newLocation, _ ->
            val userId = getUserId()
            dao.createLocation(userId, newLocation)
        }

        postEndpoint(Api.Locations.Update) { location, _ ->
            val userId = getUserId()
            if (userId != location.hostId) {
                throw UnauthorizedUserException()
            }
            dao.updateLocation(userId, location)
        }

        postEndpoint(Api.Locations.Edit) { request ->
            val edit = request.body.let { edit ->
                val imageUrl = downloadExternalImage(edit.imageUrl)
                val thumbUrl = createThumb(imageUrl, edit.thumbUrl)
                edit.copy(imageUrl = imageUrl, thumbUrl = thumbUrl)
            }
            val userId = getUserId()
            edit.locationId?.let {
                dao.updateLocation(it, userId, edit)
            } ?: dao.createLocation(userId, edit)
        }

        postEndpoint(Api.Locations.ParseLocation) { request ->
            val url = request.body
            agent.readHtml(url, locationInstructions)
        }
    }
}

val locationInstructions = """
        Read the following html. We believe it is information about a venue that hosts events.
        
        Try to determine the following:
        * name: name of the venue
        * description: description of the venue
        * address: street address of the venue
        * url: web address for more information about the venue
        * eventsUrl: web address for information about upcoming events
        * imageUrl: featured image of the venue
""".trimIndent()
