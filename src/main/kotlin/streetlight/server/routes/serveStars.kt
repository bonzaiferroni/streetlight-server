package streetlight.server.routes

import kampfire.api.toUsername
import kampfire.model.toOutcome
import klutch.server.postApi
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.model.data.LightEdit
import streetlight.model.data.MultiLightEdit
import streetlight.server.model.*
import klutch.server.authGate
import klutch.server.getApi
import streetlight.server.db.tables.StarTable

fun ApiScope.serveStars() {

    getApi(Api.Stars.ReadByUsername) { endpoint ->
        val username = readParamOrNull(endpoint.username)?.toUsername() ?: return@getApi null
        dao.star.readByUsername(username).toOutcome()
    }

    authGate {
        getApi(Api.Stars.ValidateLogin) {
            val username = call.getIdentity().username
            dao.star.readByUsername(username).toOutcome()
        }

        postApi(Api.Stars.EditStar) {
            val edit = it.data
            val callerId = call.getIdentity().callerId
            val image = checkImageAndStore(callerId, callerId, edit.image, StarTable.imageConfig)
            dao.star.updateStar(callerId, edit.copy(image = image)).toOutcome()
        }

        postApi(Api.Stars.EditLight) {
            val callerId = call.getIdentity().callerId
            when (val request = it.data) {
                is LightEdit -> {
                    dao.light.editLight(request, callerId)
                }
                is MultiLightEdit -> {
                    dao.light.editLights(request.edits, callerId)
                }
            }.toOutcome()
        }

        getApi(Api.Stars.PendingEdits) {
            val callerId = call.getIdentity().callerId
            dao.editLog.readEdits(callerId).toOutcome()
        }
    }
}