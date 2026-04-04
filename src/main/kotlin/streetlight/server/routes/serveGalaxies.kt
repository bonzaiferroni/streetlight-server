package streetlight.server.routes

import io.ktor.server.routing.Routing
import kabinet.console.globalConsole
import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.postEndpoint
import klutch.utils.getUserId
import klutch.utils.getUserIdentity
import streetlight.model.Api
import streetlight.model.data.PostListing
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

    postEndpoint(Api.Galaxies.ReadMultiPosts) {
        val galaxyIds = it.data
        app.dao.eventPost.readPosts(galaxyIds)
    }

    getEndpoint(Api.Galaxies.ReadPost, { it.toProjectId() }) { galaxyPostId, _ ->
        app.dao.eventPost.readPost(galaxyPostId)
    }

    getEndpoint(Api.Galaxies.ReadPosts, { it.toProjectId()}) { galaxyId, _ ->
        val events = app.dao.eventPost.readPosts(galaxyId).takeIf { it.isNotEmpty() }
        val locations = app.dao.locationPost.readPosts(galaxyId).takeIf { it.isNotEmpty() }
        PostListing(
            events = events,
            locations = locations
        )
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

        postEndpoint(Api.Galaxies.PostEvent) { request ->
            val identity = getUserIdentity()

            app.dao.eventPost.create(request.data, identity)
        }

        getEndpoint(Api.Galaxies.ReadLights) {
            val userId = getUserId()
            dao.readGalaxyLights(userId)
        }

        postEndpoint(Api.Galaxies.EditLight) {
            val userId = getUserId()
            dao.editGalaxyLight(it.data, userId)
        }
    }
}
