package streetlight.server.routes

import io.ktor.server.routing.Routing
import kabinet.console.globalConsole
import kampfire.model.GeoPoint
import kampfire.model.kilometers
import klutch.server.*
import klutch.utils.getUserIdOrNull
import klutch.utils.getUserIdentityOrNull
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

    authenticateJwt(optional = true) {
        postEndpoint(Api.Locations.PostLocation) { request ->
            val identity = getUserIdentityOrNull()
            app.dao.locationPost.createPost(request.data, identity)
        }

        postEndpoint(Api.Locations.PostGalaxyLocation) { request ->
            val post = request.data.takeIf { it.isValid } ?: error("invalid request")
            val identity = getUserIdentityOrNull()
            app.dao.locationPost.createGalaxyPost(post, identity)
        }

        postEndpoint(Api.Locations.CreateOrEdit) { request ->
            val edit = request.data.let { edit ->
                val imageUrl = downloadExternalImage(edit.imageUrl)
                val thumbUrl = createThumbIfNull(imageUrl, edit.thumbUrl, null)
                edit.copy(imageUrl = imageUrl, thumbUrl = thumbUrl)
            }
            val userId = getUserIdOrNull()
            edit.locationId?.let {
                dao.updateLocation(it, userId, edit)
            } ?: dao.createLocation(userId, edit)
        }
    }

    authenticateJwt {


        postEndpoint(Api.Locations.ParseLocation) { request ->
            reader.serve(request.data)
        }
    }
}


