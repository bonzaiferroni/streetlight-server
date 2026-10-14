package streetlight.server.routes

import streetlight.server.service.readFeed
import kampfire.model.HttpProblem
import kampfire.model.Ok
import klutch.server.*
import streetlight.model.Api
import streetlight.server.model.*

fun ApiScope.serveFeeds() {
    authGate(optional = true) {
        postApi(Api.Feeds.Read) { request ->
            val callerId = call.getIdentityOrNull()?.callerId
            Ok(readFeed(request.data, callerId) ?: return@postApi HttpProblem.BadRequest)
        }
    }
}
