package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.model.GeoPoint
import kampfire.model.kilometers
import klutch.server.*
import klutch.utils.getUserIdOrNull
import klutch.utils.getUserIdentityOrNull
import streetlight.model.Api
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.EventTable
import streetlight.server.model.*

private val console = globalConsole.getHandle(StreetlightRouting::serveLocations.name)

fun StreetlightRouting.serveLocations() {
    val dao = app.dao.location
    val reader = LocationScoutParser(app)

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
            val edit = request.data
            val userId = getUserIdOrNull()

            val imageUserId = userId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.locationId, edit.imageRef, EventTable.imageConfig)

            edit.locationId?.let {
                dao.updateLocation(it, userId, edit, imageSet)
            } ?: dao.createLocation(userId, edit, imageSet)
        }
    }

    authenticateJwt {
        postEndpoint(Api.Locations.ParseLocation) { request ->
            reader.serve(request.data)
        }
    }
}


