package streetlight.server.routes

import io.ktor.server.routing.Routing
import kabinet.console.globalConsole
import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.postEndpoint
import kotlinx.datetime.Clock
import streetlight.model.Api
import streetlight.model.data.GalaxyPost
import streetlight.model.data.GalaxyPostId
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider

private val console = globalConsole.getHandle(Routing::serveGalaxies.name)

fun Routing.serveGalaxies(app: ServerProvider = RuntimeProvider) {
    val dao = app.dao.galaxy

    getEndpoint(Api.Galaxies.All) {
        dao.readGalaxies()
    }

    getEndpoint(Api.Galaxies.Path) {
        val pathId = it.data
        dao.readGalaxyByPath(pathId)
    }

    authenticateJwt {
        postEndpoint(Api.Galaxies.Found) { request ->
            val galaxy = request.data
            console.log("founding galaxy: ${galaxy.name}")
            val thumbUrl = galaxy.thumbUrl ?: galaxy.imageUrl?.let {
                console.log("creating thumbnail img")
                createThumb(it)
            }
            dao.create(galaxy.copy(thumbUrl = thumbUrl))
        }

        postEndpoint(Api.Galaxies.CreatePost) { request ->
            app.dao.galaxyPost.create(request.data)
        }
    }
}
