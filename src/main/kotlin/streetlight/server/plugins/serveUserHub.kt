package streetlight.server.plugins

import io.ktor.server.routing.Routing
import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.postEndpoint
import klutch.utils.getUserId
import streetlight.model.Api
import streetlight.model.data.FileUse
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider
import streetlight.server.routes.createThumbFromUploadedImage
import streetlight.server.routes.saveBytesAsThumb
import streetlight.server.routes.saveFullImage

fun Routing.serveUserHub(app: ServerProvider = RuntimeProvider) {
    authenticateJwt {
        getEndpoint(Api.Users.Files) {
            val userId = getUserId()
            app.dao.userFile.readUserFiles(userId, FileUse.FullImage, 100).map { it.url }
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
            val bytes = it.data
            val userId = getUserId()
            saveBytesAsThumb(bytes, "${userId.value}_avatar", userId)
        }


        postEndpoint(Api.Users.UploadImage) { bytes, _ ->
            val userId = getUserId()
            saveFullImage(bytes, userId)
        }
    }
}

