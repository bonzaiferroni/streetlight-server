package streetlight.server.routes

import klutch.server.postEndpoint
import streetlight.model.Api
import streetlight.server.model.*

fun StreetlightRouting.serveRequests() {

    postEndpoint(Api.RequestBox) { newRequest, _ ->
        app.dao.request.createRequest(newRequest)?.requestId
    }
}