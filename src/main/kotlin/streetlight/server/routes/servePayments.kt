package streetlight.server.routes

import klutch.server.authGate
import klutch.server.postApi
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity
import streetlight.server.service.createPayment

fun ApiScope.servePayments() {
    authGate {
        postApi(Api.Payments.Create) {
            createPayment(call.getIdentity().callerId, it.data)
        }
    }
}
