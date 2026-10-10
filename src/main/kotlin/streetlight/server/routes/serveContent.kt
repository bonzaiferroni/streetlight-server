package streetlight.server.routes

import kampfire.model.Ok
import kampfire.model.toOutcome
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentityOrNull
import streetlight.server.model.readCityListContent
import streetlight.server.model.readHomeContent

fun ApiScope.serveContent() {

    authGate(optional = true) {
        getApi(Api.Content.Home) { endpoint ->
            val identity = call.getIdentityOrNull()
            readHomeContent(identity?.callerId, readParamOrNull(endpoint.feed)).toOutcome()
        }

        getApi(Api.Content.CityList) {
            readCityListContent().toOutcome()
        }
    }
}