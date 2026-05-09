package streetlight.server.routes

import kampfire.utils.randomUuidString
import klutch.server.getEndpoint
import klutch.server.postEndpoint
import streetlight.model.Api
import streetlight.server.model.*
import streetlight.server.plugins.authGate

fun StreetlightRouting.serveUserHub() {
    authGate {
        getEndpoint(Api.Users.Files) {
            error("not implemented")
//            val userId = getUserId()
//            app.dao.userFile.readUserFiles(userId, 100).map { it.url }
        }

        getEndpoint(Api.Users.Talents) { _ ->
            val userId = call.getIdentity().starId
            server.dao.talent.readUserTalents(userId)
        }

        postEndpoint(Api.Users.EditTalent) {
            val userId = call.getIdentity().starId
            val talentId = it.data.talentId
            if (talentId != null) {
                server.dao.talent.edit(talentId, it.data, userId)
            } else {
                server.dao.talent.create(it.data, userId)
            }
        }

        postEndpoint(Api.Users.UploadAvatar) {
            error("not implemented")
//            val bytes = it.data
//            val userId = getUserId()
            // saveBytesAsThumb(bytes, "${userId.value}_avatar", userId)
        }


        postEndpoint(Api.Users.UploadImage) {
            val userId = call.getIdentity().starId
            saveLocalImageFile(it.data, userId, randomUuidString())
        }
    }
}

