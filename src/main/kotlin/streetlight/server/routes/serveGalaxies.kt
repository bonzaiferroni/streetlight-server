package streetlight.server.routes

import io.ktor.server.routing.Routing
import kabinet.console.globalConsole
import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.postEndpoint
import klutch.utils.getUserId
import klutch.utils.getUserIdOrNull
import klutch.utils.getUsername
import streetlight.model.Api
import streetlight.model.data.toProjectId
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider

private val console = globalConsole.getHandle(Routing::serveGalaxies.name)

fun Routing.serveGalaxies(app: ServerProvider = RuntimeProvider) {
    val dao = app.dao.galaxy

    getEndpoint(Api.Galaxies.Top) {
        dao.readTopGalaxies()
    }

    getEndpoint(Api.Galaxies.Path) {
        val pathId = it.data
        dao.readGalaxyByPath(pathId)
    }
    
    postEndpoint(Api.Galaxies.ReadGalaxies) {
        val galaxyIds = it.data
        dao.readGalaxies(galaxyIds)
    }

    authenticateJwt(optional = true) {
        postEndpoint(Api.Galaxies.ReadMultiPosts) {
            val galaxyIds = it.data
            val userId = getUserIdOrNull()
            app.dao.eventPost.readPosts(galaxyIds, userId)
        }

        getEndpoint(Api.Galaxies.ReadPost, { it.toProjectId() }) { galaxyPostId, _ ->
            val userId = getUserIdOrNull()
            app.dao.eventPost.readPost(galaxyPostId, userId)
        }

        getEndpoint(Api.Galaxies.ReadPosts, { it.toProjectId()}) { galaxyId, _ ->
            val userId = getUserIdOrNull()
//        console.log(getUserId())
            app.dao.eventPost.readPosts(galaxyId, userId)
        }
    }

    authenticateJwt {
        postEndpoint(Api.Galaxies.Found) { request ->
            val galaxy = request.data
            console.log("founding galaxy: ${galaxy.name}")
            val thumbUrl = galaxy.thumbUrl ?: galaxy.imageUrl?.let {
                console.log("creating thumbnail img")
                createThumbFromUploadedImage(it, null)
            }
            dao.create(galaxy.copy(thumbUrl = thumbUrl)).also { println(it) }
        }

        postEndpoint(Api.Galaxies.CreatePost) { request ->
            val post = request.data.copy(username = getUsername())

            // td: support anonymous posts
//            if (post.username != null && post.username != getUsername()) {
//                call.respond(HttpStatusCode.Forbidden)
//                return@postEndpoint null
//            }
            app.dao.eventPost.create(post)
        }

        getEndpoint(Api.Galaxies.ReadStars) {
            val userId = getUserId()
            dao.readGalaxyStars(userId)
        }
    }
}
