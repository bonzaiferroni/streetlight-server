package streetlight.server.plugins

import kampfire.model.toOutcome
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentityOrNull

fun ApiScope.serveFeedback() {
    authGate(optional = true) {
        postApi(Api.Feedback.Create) {
            val identity = call.getIdentityOrNull()
            dao.feedback.create(it.data, identity?.callerId).toOutcome()
        }

        getApi(Api.Feedback.Feed) {
            val identity = call.getIdentityOrNull()
            dao.feedback.feed(identity).toOutcome()
        }
    }
}