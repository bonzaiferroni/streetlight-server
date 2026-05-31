package streetlight.server.routes

import kampfire.model.Ok
import klutch.server.ApiContext
import klutch.server.authGate
import klutch.server.getApi
import streetlight.model.Api
import streetlight.server.model.ContentService
import streetlight.server.model.getIdentityOrNull

fun ApiContext.serveContent() {
    val content = server.get<ContentService>()

    authGate(optional = true) {
        getApi(Api.Content.Home) {
            val identity = call.getIdentityOrNull()
            Ok(content.readHomeContent(identity?.starId))
        }
    }
}