package streetlight.server.routes

import streetlight.server.service.OmniService
import streetlight.server.service.readLocationContent
import streetlight.server.service.readLocationUpdaterContent
import streetlight.server.service.readGatherStatus
import io.ktor.server.routing.RoutingCall
import kabinet.console.globalConsole
import kampfire.model.GeoPoint
import kampfire.model.HttpProblem
import kampfire.model.Ok
import kampfire.model.kilometers
import kampfire.model.toOutcome
import klutch.db.model.Identity
import klutch.server.*
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.model.utils.expandAddress
import streetlight.server.model.*
import klutch.server.authGate
import streetlight.server.service.createLocation
import streetlight.server.service.updateLocation
import streetlight.server.utils.TimeZones

private val console = globalConsole.getHandle(ApiScope::serveLocations.name)

fun ApiScope.serveLocations() {
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
        }.toOutcome()
    }

    getApi(Api.Locations.Match) { endpoint ->
        val name = readParam(endpoint.name).trim()
        val address = readParam(endpoint.address)?.trim()?.takeIf { it.isNotBlank() }?.expandAddress()
        val city = readParam(endpoint.city)?.trim()?.takeIf { it.isNotBlank() }

        if (name.isBlank() && address == null) {
            emptyList()
        } else {
            dao.location.matchLocations(name.takeIf { it.isNotBlank() }, address, city)
        }.toOutcome()
    }

    getApi(Api.Locations.ReadTop) { endpoint ->
        val count = readParam(endpoint.count)
        dao.location.readTop(count).toOutcome()
    }

    getApi(Api.Locations.QueryPoint, GeoPoint::fromQuery) {
        val sent = it.data
        dao.location.readNearbyLocations(sent, 1.kilometers).toOutcome()
    }

    postApi(Api.Locations.QueryBounds) { request ->
        dao.location.readLocationsInBounds(request.data).toOutcome()
    }

    authGate(optional = true) {


        // when (val locationId = edit.locationId) {
        //                null -> dao.location.createLocation(starId, edit, imageSet)
        //                else -> dao.location.updateLocation(locationId, starId, edit, imageSet)
        //            }.toResponse()

        getApi(Api.Locations.ReadContent) {
            val slug = it.data
            val identity = call.getIdentityOrNull()
            readLocationContent(slug, identity).toOutcome()
        }

        getApi(Api.Locations, { it.toRecordId() }) {
            val id = it.data
            val identity = call.getIdentityOrNull()
            dao.location.readLocation(id, identity?.callerId).toOutcome()
        }

        getApi(Api.Locations.ReadLocation) {
            val slug = it.data
            val identity = call.getIdentityOrNull()
            dao.location.readLocation(slug, identity?.callerId).toOutcome()
        }
    }

    authGate {

        postApi(Api.Locations.CreateLocation) { request ->
            val edit = request.data
            val identity = call.getIdentity()
            createLocation(identity.callerId, edit)
        }

        postApi(Api.Locations.UpdateLocation) { request ->
            val edit = request.data
            val locationId = requireNotNull(edit.locationId)
            val identity = call.getIdentity()
            updateLocation(locationId, identity.callerId, edit, isAdmin = identity.isAdmin)
        }

        getApi(Api.Locations.ReadHosted) {
            val identity = call.getIdentity()
            dao.location.readHostedLocations(identity.callerId).toOutcome()
        }

        getApi(Api.Locations.ReadHostedConfigs) {
            val identity = call.getIdentity()
            dao.location.readHostedConfigContents(identity.callerId).toOutcome()
        }

        getApi(Api.Locations.ReadUpdaterContent) {
            val slug = it.data
            readLocationUpdaterContent(slug).toOutcome()
        }

        getApi(Api.Locations.ReadConfigContent, { it.toRecordId() }) {
            val hostId = call.getIdentity().hostIdOrNull()
            dao.location.readConfigContent(it.data, hostId)?.let { content -> Ok(content) }
                ?: HttpProblem.NotAuthorized
        }

        postApi(Api.Locations.ParseEventSchema) {
            HttpProblem.NotImplemented
        }

        postApi(Api.Locations.UpdateConfig) {
            val hostId = call.getIdentity().hostIdOrNull()
            if (dao.location.update(it.data, hostId) == 1) Ok(Unit)
            else HttpProblem.NotAuthorized
        }

        getApi(Api.Locations.ReadGatherStatus, { it.toRecordId() }) {
            val hostId = call.getIdentity().hostIdOrNull()
            readGatherStatus(it.data, hostId)?.let { status -> Ok(status) } ?: HttpProblem.NotAuthorized
        }

        postApi(Api.Locations.UpdateParseConfig) {
            val hostId = call.getIdentity().hostIdOrNull()
            if (dao.location.update(it.data, hostId) == 1) Ok(Unit)
            else HttpProblem.NotAuthorized
        }

        postApi(Api.Locations.UploadSchemas) {
            HttpProblem.NotImplemented
        }
    }
}

/** The caller as the host a location must have, or `null` for an admin, who may act on any location. */
fun Identity.hostIdOrNull() = callerId.takeUnless { isAdmin }

/** The caller's identity; throws when the caller is not an admin. */
fun RoutingCall.requireAdminIdentity(): Identity {
    val identity = getIdentity()
    if (!identity.isAdmin) throw UnauthorizedUserException()
    return identity
}
