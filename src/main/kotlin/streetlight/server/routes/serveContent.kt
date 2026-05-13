package streetlight.server.routes

import kampfire.model.Ok
import klutch.server.ApiContext
import klutch.server.getApi
import streetlight.model.Api
import streetlight.server.model.ContentService

fun ApiContext.serveContent() {
    val content = server.get<ContentService>()

    getApi(Api.Content.Home) {
        Ok(content.readHomeContent())
    }
}