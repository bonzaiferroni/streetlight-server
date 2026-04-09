package streetlight.server.routes

import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.postEndpoint
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.server.db.tables.EventTable
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

        postEndpoint(Api.Stars.EditStar) {
            val edit = it.data
            val starId = identity.getUserId(call)
            val imageSet = saveImages(starId, starId, edit.imageRef, EventTable.imageConfig)
            dao.updateStar(starId, edit, imageSet)
        }
    }
}