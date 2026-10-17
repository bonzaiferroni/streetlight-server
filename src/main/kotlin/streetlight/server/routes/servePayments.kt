package streetlight.server.routes

import kampfire.model.toOutcome
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity
import streetlight.server.service.createPayment
import streetlight.server.utils.starId

fun ApiScope.servePayments() {
    authGate {
        getApi(Api.Payments.ReadMine) {
            dao.payment.readStarPayments(call.getIdentity().starId).toOutcome()
        }

        getApi(Api.Customers.ReadMine) {
            dao.customer.readStarCustomers(call.getIdentity().starId).toOutcome()
        }

        postApi(Api.Payments.Create) {
            createPayment(call.getIdentity().callerId, it.data)
        }
    }
}
