package streetlight.server.routes

import klutch.server.postEndpoint
import streetlight.model.Api
import streetlight.server.model.*

fun StreetlightRouting.serveRequests() {

    postEndpoint(Api.RequestBox) {
        app.dao.request.createRequest(it.data)?.requestId
    }
}