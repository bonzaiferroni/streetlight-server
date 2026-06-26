package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.model.GeoPoint
import kampfire.model.kilometers
import kampfire.model.toResponse
import klutch.server.*
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.server.model.*
import klutch.server.authGate
import streetlight.model.data.CityId
import streetlight.model.data.LocationEdit
import streetlight.server.db.services.createLocation
import streetlight.server.db.services.readOrCreateCity
import streetlight.server.db.services.updateLocation
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.SavedImageSet

private val console = globalConsole.getHandle(ApiScope::serveLocations.name)

fun ApiScope.serveLocations() {
    val parser = provide<LocationParser>()
    val omni = provide<OmniService>()

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

    getApi(Api.Locations.ReadUpdaterContent) {
        val slug = it.data
        readLocationUpdaterContent(slug).toResponse()
    }

    authGate(optional = true) {


        // when (val locationId = edit.locationId) {
        //                null -> dao.location.createLocation(starId, edit, imageSet)
        //                else -> dao.location.updateLocation(locationId, starId, edit, imageSet)
        //            }.toResponse()

        postApi(Api.Locations.ParseLocation) { request ->
            parser.parseLocation(request.data)
        }

        getApi(Api.Locations.ReadContent) {
            val slug = it.data
            val identity = call.getIdentityOrNull()
            readLocationContent(slug, identity).toResponse()
        }

        getApi(Api.Locations, { it.toRecordId() }) {
            val id = it.data
            val identity = call.getIdentityOrNull()
            dao.location.readLocation(id, identity?.starId).toResponse()
        }

        getApi(Api.Locations.ReadLocation) {
            val slug = it.data
            val identity = call.getIdentityOrNull()
            dao.location.readLocation(slug, identity?.starId).toResponse()
        }
    }

    authGate(optional = false) {
//        suspend fun <T> handleEdit(
//            edit: LocationEdit,
//            identity: StarIdentity,
//            block: suspend (CityId, SavedImageSet?) -> T?
//        ): T? {
//            val imageUserId = identity.starId.takeIf { edit.imageRef?.isRelative ?: false }
//            val imageSet = saveImages(imageUserId, edit.locationId, edit.imageRef, EventTable.imageConfig)
//            val cityId = readOrCreateCity(edit.city, edit.state)
//            return block(requireNotNull(cityId) { "city not found" }, imageSet)
//        }

        postApi(Api.Locations.CreateLocation) { request ->
            val edit = request.data
            val identity = call.getIdentity()
            createLocation(identity.starId, edit).toResponse()
        }

        postApi(Api.Locations.UpdateLocation) { request ->
            val edit = request.data
            val locationId = requireNotNull(edit.locationId)
            val identity = call.getIdentity()
            updateLocation(locationId, identity.starId, edit).toResponse()
        }

    }
}


