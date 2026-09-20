package streetlight.server.plugins

import kabinet.utils.Environment
import kampfire.model.CoreProblem
import kampfire.model.Ok
import klutch.server.authGate
import klutch.server.postApi
import klutch.server.provide
import streetlight.model.Api
import streetlight.server.buildId
import streetlight.server.buildMode
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentityOrNull

fun ApiScope.serveBug() {
    val buildId = provide<Environment>().buildMode.buildId

    authGate(optional = true) {
        postApi(Api.Bugs.Report) {
            val identity = call.getIdentityOrNull()
            if (!dao.bug.create(it.data, identity?.callerId, buildId)) return@postApi CoreProblem.Something
            Ok(Unit)
        }
    }
}
