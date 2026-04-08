package streetlight.server.routes

import kampfire.utils.randomUuidString
import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.postEndpoint
import klutch.utils.getUserId
import streetlight.model.Api
import streetlight.model.data.StorageType
import streetlight.server.model.*

fun StreetlightRouting.serveUserHub() {
    authenticateJwt {
        getEndpoint(Api.Users.Files) {
            error("not implemented")
//            val userId = getUserId()
//            app.dao.userFile.readUserFiles(userId, 100).map { it.url }
        }

        getEndpoint(Api.Users.Talents) { _ ->
            val userId = getUserId()
            app.dao.talent.readUserTalents(userId)
        }

        postEndpoint(Api.Users.EditTalent) {
            val userId = getUserId()
            val talentId = it.data.talentId
            if (talentId != null) {
                app.dao.talent.edit(talentId, it.data, userId)
            } else {
                app.dao.talent.create(it.data, userId)
            }
        }

        postEndpoint(Api.Users.UploadAvatar) {
            error("not implemented")
//            val bytes = it.data
//            val userId = getUserId()
            // saveBytesAsThumb(bytes, "${userId.value}_avatar", userId)
        }


        postEndpoint(Api.Users.UploadImage) { bytes, _ ->
            val userId = getUserId()
            saveLocalImageFile(bytes, userId, randomUuidString())
        }
    }
}

