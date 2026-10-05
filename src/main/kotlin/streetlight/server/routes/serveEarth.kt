package streetlight.server.routes

import kampfire.model.HttpProblem
import kampfire.model.Ok
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.readParam
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentityOrNull
import streetlight.server.model.readEarthFeed
import streetlight.server.model.readInflateFeed

fun ApiScope.serveEarth() {
    getApi(Api.EarthNode.Query) {
        val query = it.mapQuery() ?: return@getApi HttpProblem.BadRequest
        Ok(readEarthFeed(query))
    }

    authGate(optional = true) {
        getApi(Api.EarthNode.Inflate) {
            val locationIds = readParam(it.locationIdsParam)
            val cursor = it.readTimeCursor()
            Ok(readInflateFeed(locationIds, call.getIdentityOrNull()?.callerId, cursor))
        }
    }
}