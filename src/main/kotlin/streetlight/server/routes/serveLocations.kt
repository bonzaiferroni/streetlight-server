package streetlight.server.routes

import io.ktor.server.routing.Routing
import kampfire.model.GeoPoint
import kampfire.model.kilometers
import klutch.server.*
import klutch.utils.getUserId
import streetlight.model.Api
import streetlight.model.data.toProjectId
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider

fun Routing.serveLocations(app: ServerProvider = RuntimeProvider) {
    val dao = app.dao.location
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
            val edit = request.body
            val userId = getUserId()
            edit.locationId?.let {
                dao.updateLocation(it, userId, edit)
            } ?: dao.createLocation(userId, edit)
        }
    }
}