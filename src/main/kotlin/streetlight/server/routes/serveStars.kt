package streetlight.server.routes

import kampfire.model.Ok
import kampfire.model.Problem
import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.postApi
import klutch.server.postEndpoint
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.model.data.LightEdit
import streetlight.model.data.MultiLightEdit
import streetlight.server.db.tables.EventTable
import streetlight.server.model.*
import streetlight.server.model.dao

fun StreetlightRouting.serveStars() {

    getEndpoint(Api.Stars.ReadByUsername) { endpoint ->
        val username = readParamOrNull(endpoint.username) ?: return@getEndpoint null
        dao.star.readByUsername(username)
    }

    authenticateJwt {
        getEndpoint(Api.Stars.ValidateLogin) {
            val username = identity.getUsername(call)
            dao.star.readByUsername(username)
        }

        postEndpoint(Api.Stars.EditStar) {
            val edit = it.data
            val starId = identity.getUserId(call)
            val imageSet = saveImages(starId, starId, edit.imageRef, EventTable.imageConfig)
            dao.star.updateStar(starId, edit, imageSet)
        }

        postApi(Api.Stars.EditLight) {
            val starId = identity.getUserId(call)
            val isSuccess = when (val request = it.data) {
                is LightEdit -> {
                    dao.light.editLight(request, starId)
                }
                is MultiLightEdit -> {
                    dao.light.editLights(request.edits, starId)
                }
            }
            when (isSuccess) {
                true -> Ok(Unit)
                false -> Problem("Something went wrong.")
            }
        }
    }
}