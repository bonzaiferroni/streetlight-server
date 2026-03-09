package streetlight.server.routes

import io.ktor.server.routing.Routing
import kabinet.console.globalConsole
import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.postEndpoint
import streetlight.model.Api
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider

private val console = globalConsole.getHandle(Routing::serveGalaxies.name)

fun Routing.serveGalaxies(app: ServerProvider = RuntimeProvider) {
    val dao = app.dao.galaxy

    getEndpoint(Api.Galaxies.All) {
        dao.readGalaxies()
    }

    authenticateJwt {
        postEndpoint(Api.Galaxies.Found) { request ->
            console.log("founding galaxy: ${request.body.name}")
            dao.create(request.body)
        }
    }
}
