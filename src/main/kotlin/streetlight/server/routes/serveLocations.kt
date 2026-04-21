package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.model.GeoPoint
import kampfire.model.kilometers
import klutch.server.*
import streetlight.model.Api
import streetlight.model.data.LocationCreated
import streetlight.model.data.LocationEdited
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.LocationTable
import streetlight.server.model.*
import kotlin.time.Clock

private val console = globalConsole.getHandle(StreetlightRouting::serveLocations.name)

fun StreetlightRouting.serveLocations() {
    val dao = server.dao.location
    val reader = LocationParser(server)

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
            val identity = identity.getIdentityOrNull(call)
            server.dao.locationPost.createPost(request.data, identity)
        }

        postEndpoint(Api.Locations.PostGalaxyLocation) { request ->
            val post = request.data.takeIf { it.isValid } ?: error("invalid request")
            val identity = identity.getIdentityOrNull(call)
            server.dao.locationPost.createGalaxyPost(post, identity)
        }

        postEndpoint(Api.Locations.CreateOrEdit) { request ->
            val edit = request.data
            val identity = identity.getIdentityOrNull(call)
            val starId = identity?.userId

            val imageUserId = starId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.locationId, edit.imageRef, LocationTable.imageConfig)

            edit.locationId?.let {
                val location = dao.updateLocation(it, starId, edit, imageSet)
                server.service.omni.sendMessage(LocationEdited(
                    locationId = location.locationId,
                    name = location.name,
                    username = identity?.username,
                    recordAt = Clock.System.now()
                ))
                location
            } ?: dao.createLocation(starId, edit, imageSet).also {
                server.service.omni.sendMessage(LocationCreated(
                    locationId = it.locationId,
                    name = it.name,
                    username = identity?.username,
                    recordAt = Clock.System.now()
                ))
            }
        }

        postApi(Api.Locations.ParseLocation) { request ->
            reader.parseLocation(request.data)
        }
    }
}


