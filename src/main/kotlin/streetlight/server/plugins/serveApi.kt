package streetlight.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import klutch.server.*
import streetlight.server.db.services.StarAuthDao
import klutch.server.routingContextOf
import streetlight.server.model.getIdentity
import streetlight.server.routes.*

fun Application.serveApi(server: ServerContext) {
    val authDao = StarAuthDao()
    // val identity = Identity(authDao)
    routing {
        routingContextOf(server) {
            serveUserAuth(
                dao = authDao,
                authGate = ::authGate,
                getUsername = { it.getIdentity().username },
                getUserId = { it.getIdentity().starId }
            )

            serveEvents()
            serveGalaxies()
            serveStars()
            serveLocations()
            serveSongs()
            serveRenditions()
            servePages()
            serveGtfs()
            serveRequests()
            // serveGemini(Api.Gemini, app.gemini)
            // serveSpeech(Api.Speech, app.ai.speech)
            serveUserHub()
            serveChat()
            serveMap()
            serveFiles()
            serveOmni()
            serveSiteDocs()
            serveTalk()
            serveContent()
            serveCity()
        }
    }
}