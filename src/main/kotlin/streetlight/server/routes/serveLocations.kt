package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.api.Slug
import kampfire.model.GeoPoint
import kampfire.model.kilometers
import kampfire.model.toResponse
import klutch.server.*
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.server.model.*
import klutch.server.authGate
import streetlight.model.data.CityId
import streetlight.model.data.Location
import streetlight.model.data.LocationEdit
import streetlight.server.db.services.CityService
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.SavedImageSet

private val console = globalConsole.getHandle(ApiContext::serveLocations.name)

fun ApiContext.serveLocations() {
    val parser = server.get<LocationParser>()
    val omni = server.get<OmniService>()
    val cityService = server.get<CityService>()

    getApi(Api.Locations, { it.toRecordId() }) {
        val id = it.data
        dao.location.readLocation(id).toResponse()
    }

    getApi(Api.Locations.ReadSlug) {
        val slug = it.data
        dao.location.readSlug(slug).toResponse()
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
        suspend fun <T> handleEdit(
            edit: LocationEdit,
            identity: StarIdentity?,
            block: suspend (CityId, SavedImageSet) -> T?
        ): T? {
            val imageUserId = identity?.starId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.locationId, edit.imageRef, EventTable.imageConfig)
            val cityId = cityService.readOrCreateCity(edit.city, edit.state)
            return block(requireNotNull(cityId), requireNotNull(imageSet))
        }

        postApi(Api.Locations.CreateLocation) { request ->
            val edit = request.data
            val identity = call.getIdentityOrNull()

            handleEdit(edit, identity) { cityId, imageSet ->
                dao.location.createLocation(cityId, identity?.starId, edit, imageSet)
            }.toResponse()
        }

        postApi(Api.Locations.UpdateLocation) { request ->
            val edit = request.data
            val identity = call.getIdentityOrNull()
            val locationId = requireNotNull(edit.locationId)

            handleEdit(edit, identity) { cityId, imageSet ->
                dao.location.updateLocation(locationId, cityId, identity?.starId, edit, imageSet)
            }.toResponse()
        }

        // when (val locationId = edit.locationId) {
        //                null -> dao.location.createLocation(starId, edit, imageSet)
        //                else -> dao.location.updateLocation(locationId, starId, edit, imageSet)
        //            }.toResponse()

        postApi(Api.Locations.ParseLocation) { request ->
            parser.parseLocation(request.data)
        }
    }
}


