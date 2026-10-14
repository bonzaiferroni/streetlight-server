package streetlight.server.routes

import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity
import streetlight.server.service.createOffering
import streetlight.server.service.readStarOfferings
import streetlight.server.service.updateOffering
import streetlight.server.utils.starId

fun ApiScope.serveOfferings() {
    authGate {
        getApi(Api.Offerings.ReadMine) {
            readStarOfferings(call.getIdentity().starId)
        }

        postApi(Api.Offerings.Create) {
            createOffering(call.getIdentity().callerId, it.data)
        }

        postApi(Api.Offerings.Update) {
            updateOffering(call.getIdentity().callerId, it.data)
        }
    }
}
