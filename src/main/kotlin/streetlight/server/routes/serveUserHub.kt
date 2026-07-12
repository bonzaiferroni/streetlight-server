package streetlight.server.routes

import kampfire.model.toOutcome
import kampfire.utils.randomUuidString
import streetlight.model.Api
import streetlight.server.model.*
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi

fun ApiScope.serveUserHub() {
    authGate {
        getApi(Api.Users.Files) {
            error("not implemented")
//            val userId = getUserId()
//            app.dao.userFile.readUserFiles(userId, 100).map { it.url }
        }

        getApi(Api.Users.Talents) { _ ->
            val userId = call.getIdentity().callerId
            dao.talent.readUserTalents(userId).toOutcome()
        }

        postApi(Api.Users.EditTalent) {
            val userId = call.getIdentity().callerId
            val talentId = it.data.talentId
            if (talentId != null) {
                dao.talent.edit(talentId, it.data, userId)
            } else {
                dao.talent.create(it.data, userId)
            }.toOutcome()
        }

        postApi(Api.Users.UploadAvatar) {
            error("not implemented")
//            val bytes = it.data
//            val userId = getUserId()
            // saveBytesAsThumb(bytes, "${userId.value}_avatar", userId)
        }


        postApi(Api.Users.UploadImage) {
            val userId = call.getIdentity().callerId
            saveLocalImageFile(it.data, userId, randomUuidString()).toOutcome()
        }
    }
}

