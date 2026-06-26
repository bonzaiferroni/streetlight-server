package streetlight.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import klutch.db.services.RefreshTokenService
import klutch.server.*
import streetlight.server.db.services.StarAuthDao
import streetlight.server.model.ServerRouting
import streetlight.server.model.ServerScope
import streetlight.server.model.getIdentity
import streetlight.server.routes.*

fun Application.serveApi(server: ServerScope) {
    val authDao = StarAuthDao()
    // val identity = Identity(authDao)
    routing {
        val scope = ServerRouting(server, this)
        with (scope) {
            serveUserAuth(
                refreshTokenService = provide<RefreshTokenService>(),
                jwtService = provide<JwtService>(),
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
            serveTasks()
        }
    }
}