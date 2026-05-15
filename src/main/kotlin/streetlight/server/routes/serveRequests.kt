package streetlight.server.routes

import kampfire.model.toResponse
import klutch.server.ApiContext
import klutch.server.postApi
import klutch.server.postEndpoint
import streetlight.model.Api
import streetlight.server.model.*

fun ApiContext.serveRequests() {

    postApi(Api.RequestBox) {
        dao.request.createRequest(it.data)?.requestId.toResponse()
    }
}