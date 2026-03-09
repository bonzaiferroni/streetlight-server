package streetlight.server.routes

import io.ktor.server.routing.Routing
import kabinet.console.globalConsole
import kampfire.model.GeoPoint
import kampfire.model.kilometers
import klutch.server.*
import klutch.utils.getUserId
import streetlight.model.Api
import streetlight.model.data.toProjectId
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider

private val console = globalConsole.getHandle(Routing::serveLocations.name)

fun Routing.serveLocations(app: ServerProvider = RuntimeProvider) {
    val dao = app.dao.location
    val reader = LocationScoutParser(app)

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

    postEndpoint(Api.Locations.QueryBounds) { request ->
        dao.readLocationsInBounds(request.data)
    }

    authenticateJwt {
        postEndpoint(Api.Locations.Edit) { request ->
            val edit = request.data.let { edit ->
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
            reader.serve(request.data)
        }
    }
}


