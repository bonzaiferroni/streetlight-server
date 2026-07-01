package streetlight.server.routes

import kampfire.model.toOutcome
import klutch.server.postApi
import streetlight.model.Api
import streetlight.server.model.*

fun ApiScope.serveRequests() {

    postApi(Api.RequestBox) {
        dao.request.createRequest(it.data)?.requestId.toOutcome()
    }
}