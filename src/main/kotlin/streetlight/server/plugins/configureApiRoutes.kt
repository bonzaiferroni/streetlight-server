package streetlight.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import klutch.gemini.serveSpeech
import klutch.server.*
import streetlight.model.Api
import streetlight.server.db.services.StarAuthDao
import streetlight.server.db.services.provideStarUser
import streetlight.server.model.Streetlight
import streetlight.server.model.routingContextOf
import streetlight.server.routes.*

fun Application.configureApiRoutes(app: Streetlight) {
    routing {
        routingContextOf(app) {
            serveUserAuth(StarAuthDao(), ::provideStarUser)
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
        }
    }
}