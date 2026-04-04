package streetlight.server.routes

import io.ktor.server.routing.Routing
import klutch.server.getEndpoint
import klutch.server.readParam
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider

fun Routing.serveStars(app: ServerProvider = RuntimeProvider) {
    val dao = app.dao.star

    getEndpoint(Api.Stars.ReadByUsername) { endpoint ->
        val username = readParamOrNull(endpoint.username) ?: return@getEndpoint null
        dao.readByUsername(username)
    }
}