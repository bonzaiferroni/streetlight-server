package streetlight.server.routes

import kampfire.api.deobfuscatePassword
import kampfire.api.toValidOutcome
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.toOutcome
import klutch.server.postApi
import streetlight.model.Api
import streetlight.model.data.LightEdit
import streetlight.model.data.MultiLightEdit
import streetlight.server.model.*
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.readParam
import streetlight.server.db.tables.StarTable
import streetlight.server.utils.toStarId

fun ApiScope.serveStars() {
    authGate(optional = true) {
        getApi(Api.Stars.ReadStarContent) {
            val username = readParam(it.username)
            val identity = call.getIdentityOrNull()
            readStarContent(username, identity)?.toOutcome()
        }

        getApi(Api.Stars.ValidateLogin) {
            val identity = call.getIdentityOrNull() ?: return@getApi Ok(null)
            dao.star.readStar(identity.username, null).toOutcome()
        }
    }

    authGate {
        postApi(Api.Stars.UpdateProfile) {
            val edit = it.data
            val callerId = call.getIdentity().callerId
            val image = checkImageAndStore(callerId, callerId, edit.image, StarTable.imageConfig)
            dao.star.updateProfile(callerId, edit.copy(image = image)).toOutcome()
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

        getApi(Api.Stars.ReadAccount) {
            val callerId = call.getIdentity().callerId
            dao.star.readAccount(callerId.toStarId()).toOutcome()
        }
    }
}