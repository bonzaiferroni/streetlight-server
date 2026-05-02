package streetlight.server.routes

import kampfire.model.Ok
import klutch.server.getApi
import streetlight.model.Api
import streetlight.server.model.StreetlightRouting
import streetlight.server.model.service

fun StreetlightRouting.serveContent() {

    getApi(Api.Content.Home) {
        Ok(service.content.readHomeContent())
    }
}