package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.model.GeoPoint
import kampfire.model.kilometers
import kampfire.model.responseOf
import kampfire.model.toResponse
import klutch.server.*
import streetlight.model.Api
import streetlight.model.data.LocationCreated
import streetlight.model.data.LocationEdited
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.LocationTable
import streetlight.server.model.*
import klutch.server.authGate
import streetlight.server.db.services.CityService
import kotlin.time.Clock

private val console = globalConsole.getHandle(ApiContext::serveLocations.name)

fun ApiContext.serveLocations() {
    val parser = server.get<LocationParser>()
    val omni = server.get<OmniService>()
    val cityService = server.get<CityService>()

    getApi(Api.Locations, { it.toProjectId() }) {
        val id = it.data
        dao.location.readLocation(id).toResponse()
    }

    getApi(Api.Locations.Search) { endpoint ->
        val query = readParam(endpoint.query)
        val city = readParam(endpoint.city)?.takeIf { it.isNotBlank() }
        val state = readParam(endpoint.state)?.takeIf { it.isNotBlank() }
        val limit = readParam(endpoint.limit)

        if (query.isBlank()) {
            emptyList()
        } else {
            dao.location.searchLocations(query, city, state, limit)
        }.toResponse()
    }

    getApi(Api.Locations.ReadTop) { endpoint ->
        val count = readParam(endpoint.count)
        dao.location.readTop(count).toResponse()
    }

    getApi(Api.Locations.QueryPoint, GeoPoint::fromQuery) {
        val sent = it.data
        dao.location.readNearbyLocations(sent, 1.kilometers).toResponse()
    }

    postApi(Api.Locations.QueryBounds) { request ->
        dao.location.readLocationsInBounds(request.data).toResponse()
    }

    authGate(optional = true) {
        postApi(Api.Locations.CreateOrEdit) { request ->
            var edit = request.data
            val identity = call.getIdentityOrNull()
            val starId = identity?.starId

            val imageUserId = starId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageRef = edit.imageRef?.takeIf { it.value.isNotBlank() }
            val imageSet = saveImages(imageUserId, edit.locationId, imageRef, LocationTable.imageConfig)
            val cityId = cityService.readOrCreateCity(edit.city, edit.state)
            edit = edit.copy(cityId = cityId)

            val location = edit.locationId?.let {
                val location = dao.location.updateLocation(it, starId, edit, imageSet)
                omni.sendMessage(LocationEdited(
                    locationId = location.locationId,
                    name = location.displayTitle,
                    username = identity?.username,
                    recordAt = Clock.System.now()
                ))
                location
            } ?: dao.location.createLocation(starId, edit, imageSet).also {
                omni.sendMessage(LocationCreated(
                    locationId = it.locationId,
                    name = it.displayTitle,
                    username = identity?.username,
                    recordAt = Clock.System.now()
                ))
            }
            responseOf(location)
        }

        postApi(Api.Locations.ParseLocation) { request ->
            parser.parseLocation(request.data)
        }
    }
}


