package streetlight.server.routes

import kampfire.api.toUsername
import kampfire.model.toResponse
import klutch.server.ApiContext
import klutch.server.postApi
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.model.data.LightEdit
import streetlight.model.data.MultiLightEdit
import streetlight.server.db.tables.EventTable
import streetlight.server.model.*
import streetlight.server.model.dao
import klutch.server.authGate
import klutch.server.getApi

fun ApiContext.serveStars() {

    getApi(Api.Stars.ReadByUsername) { endpoint ->
        val username = readParamOrNull(endpoint.username)?.toUsername() ?: return@getApi null
        dao.star.readByUsername(username).toResponse()
    }

    authGate {
        getApi(Api.Stars.ValidateLogin) {
            val username = call.getIdentity().username
            dao.star.readByUsername(username).toResponse()
        }

        postApi(Api.Stars.EditStar) {
            val edit = it.data
            val starId = call.getIdentity().starId
            val imageSet = saveImages(starId, starId, edit.imageRef, EventTable.imageConfig)
            dao.star.updateStar(starId, edit, imageSet).toResponse()
        }

        postApi(Api.Stars.EditLight) {
            val starId = call.getIdentity().starId
            when (val request = it.data) {
                is LightEdit -> {
                    dao.light.editLight(request, starId)
                }
                is MultiLightEdit -> {
                    dao.light.editLights(request.edits, starId)
                }
            }.toResponse()
        }

        getApi(Api.Stars.PendingEdits) {
            val callerId = call.getIdentity().starId
            dao.editLog.readEdits(callerId).toResponse()
        }
    }
}