package streetlight.server.routes

import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity
import streetlight.server.service.createOffering
import streetlight.server.service.readStarOffering
import streetlight.server.service.readStarOfferings
import streetlight.server.service.updateOffering
import streetlight.server.utils.starId

fun ApiScope.serveOfferings() {
    authGate {
        getApi(Api.Offerings.ReadMine) {
            readStarOfferings(call.getIdentity().starId)
        }

        getApi(Api.Offerings.Read, { it.toRecordId() }) {
            readStarOffering(call.getIdentity().starId, it.data)
        }

        postApi(Api.Offerings.Create) {
            createOffering(call.getIdentity().callerId, it.data)
        }

        postApi(Api.Offerings.Update) {
            updateOffering(call.getIdentity().callerId, it.data)
        }
    }
}
