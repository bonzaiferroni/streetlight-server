package streetlight.server.routes

import streetlight.server.service.readCityFeedContent
import streetlight.server.service.readGalaxyFeedContent
import streetlight.server.service.readHomeContent
import kampfire.model.Ok
import kampfire.model.toOutcome
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentityOrNull

fun ApiScope.serveContent() {

    authGate(optional = true) {
        getApi(Api.Content.Home) { endpoint ->
            val identity = call.getIdentityOrNull()
            readHomeContent(identity?.callerId, readParamOrNull(endpoint.feed)).toOutcome()
        }

        getApi(Api.Content.CityFeed) {
            readCityFeedContent().toOutcome()
        }

        getApi(Api.Content.GalaxyFeed) { endpoint ->
            val identity = call.getIdentityOrNull()
            readGalaxyFeedContent(identity?.callerId, readParamOrNull(endpoint.feed)).toOutcome()
        }
    }
}