package streetlight.server.routes

import klutch.server.ApiContext
import klutch.server.postEndpoint
import streetlight.model.Api
import streetlight.server.model.*

fun ApiContext.serveRequests() {

    postEndpoint(Api.RequestBox) {
        dao.request.createRequest(it.data)?.requestId
    }
}