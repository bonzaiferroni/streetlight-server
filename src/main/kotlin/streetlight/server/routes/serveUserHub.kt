package streetlight.server.routes

import kampfire.model.toResponse
import kampfire.utils.randomUuidString
import klutch.server.ApiContext
import klutch.server.getEndpoint
import klutch.server.postEndpoint
import streetlight.model.Api
import streetlight.server.model.*
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi

fun ApiContext.serveUserHub() {
    authGate {
        getApi(Api.Users.Files) {
            error("not implemented")
//            val userId = getUserId()
//            app.dao.userFile.readUserFiles(userId, 100).map { it.url }
        }

        getApi(Api.Users.Talents) { _ ->
            val userId = call.getIdentity().starId
            dao.talent.readUserTalents(userId).toResponse()
        }

        postApi(Api.Users.EditTalent) {
            val userId = call.getIdentity().starId
            val talentId = it.data.talentId
            if (talentId != null) {
                dao.talent.edit(talentId, it.data, userId)
            } else {
                dao.talent.create(it.data, userId)
            }.toResponse()
        }

        postApi(Api.Users.UploadAvatar) {
            error("not implemented")
//            val bytes = it.data
//            val userId = getUserId()
            // saveBytesAsThumb(bytes, "${userId.value}_avatar", userId)
        }


        postApi(Api.Users.UploadImage) {
            val userId = call.getIdentity().starId
            saveLocalImageFile(it.data, userId, randomUuidString()).toResponse()
        }
    }
}

