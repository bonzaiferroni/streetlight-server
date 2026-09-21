package streetlight.server.routes

import kampfire.model.Ok
import kampfire.model.toOutcome
import klutch.server.authGate
import klutch.server.getApi
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentityOrNull
import streetlight.server.model.readCityListContent
import streetlight.server.model.readHomeContent

fun ApiScope.serveContent() {

    authGate(optional = true) {
        getApi(Api.Content.Home) {
            val identity = call.getIdentityOrNull()
            readHomeContent(identity?.callerId).toOutcome()
        }

        getApi(Api.Content.CityList) {
            readCityListContent().toOutcome()
        }
    }
}