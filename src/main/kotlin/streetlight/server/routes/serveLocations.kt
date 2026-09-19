package streetlight.server.routes

import io.ktor.server.routing.RoutingCall
import kabinet.console.globalConsole
import kampfire.model.GeoPoint
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.kilometers
import kampfire.model.toOutcome
import klutch.db.model.Identity
import klutch.server.*
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.server.model.*
import klutch.server.authGate
import streetlight.agent.HtmlParserClient
import streetlight.model.data.FetchMode
import streetlight.model.data.toOriginId
import streetlight.server.db.datascope.createLocation
import streetlight.server.db.datascope.parseEventSchema
import streetlight.server.db.datascope.updateLocation
import streetlight.server.utils.TimeZones

private val console = globalConsole.getHandle(ApiScope::serveLocations.name)

fun ApiScope.serveLocations() {
    val parser = provide<LocationParser>()
    val omni = provide<OmniService>()
    val koog = provide<HtmlParserClient>()

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

        postApi(Api.Locations.ParseLocation) { request ->
            parser.parseLocation(request.data)
        }

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
            updateLocation(locationId, identity.callerId, edit)
        }

        getApi(Api.Locations.ReadUpdaterContent) {
            val slug = it.data
            readLocationUpdaterContent(slug).toOutcome()
        }

        getApi(Api.Locations.ReadConfigContent, { it.toRecordId() }) {
            call.requireAdminIdentity()
            dao.location.readConfigContent(it.data).toOutcome()
        }

        postApi(Api.Locations.ParseEventSchema) {
            call.requireAdminIdentity()
            parseEventSchema(it.data, koog)
        }

        postApi(Api.Locations.UpdateConfig) {
            call.requireAdminIdentity()
            if (dao.location.update(it.data) != 1) error("Update not applied")
            Ok(Unit)
        }

        postApi(Api.Locations.UploadSchemas) {
            call.requireAdminIdentity()
            val urlSchemas = it.data
            val originId = urlSchemas.url.toOriginId() ?: return@postApi Problem("Invalid url: ${urlSchemas.url}")
            dao.origin.readOrCreateOrigin(originId)
            urlSchemas.schemas.forEach { schema ->
                dao.parser.create(originId, schema, FetchMode.Basic)
                dao.origin.linkLocation(originId, urlSchemas.locationId)
            }
            Ok(Unit)
        }
    }
}

fun RoutingCall.requireAdminIdentity(): Identity {
    val identity = getIdentity()
    if (!identity.isAdmin) throw UnauthorizedUserException()
    return identity
}
