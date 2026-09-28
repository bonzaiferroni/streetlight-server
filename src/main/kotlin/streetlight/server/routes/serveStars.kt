package streetlight.server.routes

import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.normalize
import streetlight.model.data.LeadType
import streetlight.server.utils.starId
import kampfire.model.toDataOr
import kampfire.model.toOutcome
import klutch.server.postApi
import streetlight.model.Api
import streetlight.model.data.LightEdit
import streetlight.model.data.MultiLightEdit
import streetlight.server.model.*
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.readParam
import streetlight.model.data.ProfileConfig
import streetlight.server.db.tables.StarTable
import streetlight.server.utils.toStarId

fun ApiScope.serveStars() {
    authGate(optional = true) {
        getApi(Api.Stars.ReadStarContent) {
            val username = readParam(it.username)
            val identity = call.getIdentityOrNull()
            readStarContent(username, identity)?.toOutcome()
        }
    }

    authGate {
        getApi(Api.Stars.ValidateLogin) {
            val identity = call.getIdentity()
            dao.star.readStar(identity.username, null).toOutcome()
        }

        postApi(Api.Stars.UpdateProfile) { request ->
            val edit = request.data
            val callerId = call.getIdentity().callerId
            val image = checkImageAndStore(callerId, callerId, edit.image, StarTable.imageConfig)
                .toDataOr { return@postApi it }
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

        postApi(Api.Stars.CreateLead) { request ->
            val identity = call.requireAdminIdentity()
            val lead = request.data
            if (lead.leadType !in submittableLeadTypes) return@postApi Problem("Only location and event leads can be submitted.")
            if (!lead.url.isAbsolute) return@postApi Problem("That isn't a web address.")
            if (dao.lead.readLeadByUrl(lead.url.normalize()) != null) return@postApi Problem("That one is already on our list.")
            Ok(dao.lead.create(lead, identity.starId))
        }

        getApi(Api.Stars.ReadProfileConfig) {
            val callerId = call.getIdentity().callerId
            Ok(ProfileConfig(dao.star.readDesign(callerId)))
        }
    }
}

private val submittableLeadTypes = setOf(LeadType.Location, LeadType.Event)
