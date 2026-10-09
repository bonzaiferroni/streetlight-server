package streetlight.server.routes

import kampfire.model.HttpProblem
import kampfire.model.Ok
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.readParam
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentityOrNull
import streetlight.server.model.readEarthFeed

fun ApiScope.serveEarth() {
    getApi(Api.EarthNode.Query) {
        val query = it.mapQuery() ?: return@getApi HttpProblem.BadRequest
        Ok(readEarthFeed(query))
    }

    authGate(optional = true) {
        getApi(Api.EarthNode.Inflate) {
            val locationId = readParam(it.locationIdParam)
            val tag = readParamOrNull(it.tagParam)
            val search = readParamOrNull(it.searchParam)?.trim()?.takeIf { it.isNotEmpty() }
            Ok(dao.earth.readLocationId(locationId, call.getIdentityOrNull()?.callerId, tag, search))
        }
    }
}