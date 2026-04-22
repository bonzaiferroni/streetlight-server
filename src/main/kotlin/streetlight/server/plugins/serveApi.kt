package streetlight.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import klutch.gemini.serveSpeech
import klutch.server.*
import streetlight.model.Api
import streetlight.server.db.services.StarAuthDao
import streetlight.server.db.services.provideStarUser
import streetlight.server.model.StreetlightServer
import klutch.server.routingContextOf
import klutch.utils.Identity
import streetlight.server.routes.*

fun Application.serveApi(app: StreetlightServer) {
    val authDao = StarAuthDao()
    val identity = Identity(authDao)
    routing {
        routingContextOf(app, identity) {
            serveUserAuth(authDao, identity, StarRefreshTokenTable, ::provideStarUser)
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
            serveSpeech(Api.Speech, app.ai.speech)
            serveUserHub()
            serveChat()
            serveMap()
            serveFiles()
            serveOmni()
            serveSiteDocs()
            serveComments()
        }
    }
}