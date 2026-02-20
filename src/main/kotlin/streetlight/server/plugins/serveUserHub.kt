package streetlight.server.plugins

import io.ktor.server.routing.Routing
import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.postEndpoint
import klutch.utils.getUserId
import streetlight.model.Api
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider

fun Routing.serveUserHub(app: ServerProvider = RuntimeProvider) {
    authenticateJwt {
        postEndpoint(Api.Users.Images) { request ->
            val body = request.body
            val userId = getUserId()
            app.dao.userFile.readUserFiles(userId, body.fileUse, body.count).map { it.url }
        }

        getEndpoint(Api.Users.Talents) { _ ->
            val userId = getUserId()
            app.dao.talent.readUserTalents(userId)
        }

        postEndpoint(Api.Users.CreateTalent) {
            val userId = getUserId()
            app.dao.talent.create(it.body, userId)
        }
    }
}

