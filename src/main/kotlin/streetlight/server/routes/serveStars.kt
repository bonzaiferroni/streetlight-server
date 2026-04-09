package streetlight.server.routes

import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.server.model.*

fun StreetlightRouting.serveStars() {
    val dao = app.dao.star

    getEndpoint(Api.Stars.ReadByUsername) { endpoint ->
        val username = readParamOrNull(endpoint.username) ?: return@getEndpoint null
        dao.readByUsername(username)
    }

    authenticateJwt {
        getEndpoint(Api.Stars.ValidateLogin) {
            val username = identity.getUsername(call)
            dao.readByUsername(username)
        }
    }
}