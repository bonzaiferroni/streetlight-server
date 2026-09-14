package streetlight.server.routes

import kampfire.model.HttpProblem
import kampfire.model.toOk
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.readParam
import streetlight.model.Api
import streetlight.server.model.*

fun ApiScope.serveMap() {
    authGate(optional = true) {
        getApi(Api.Map.ReadEntities) {
            val bounds = readParam(it.bounds) ?: return@getApi HttpProblem.BadRequest
            val callerId = call.getIdentityOrNull()?.callerId
            dao.post.readMapPosts(bounds, callerId).toOk()
        }
    }
}