package streetlight.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import klutch.gemini.serveSpeech
import klutch.server.*
import streetlight.model.Api
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider
import streetlight.server.routes.*

fun Application.configureApiRoutes(app: ServerProvider = RuntimeProvider) {
    routing {
        serveUsers()
        serveEvents()
        serveGalaxies()
        serveLocations()
        serveSongs()
        serveRenditions()
        servePages()
        serveGtfs()
        serveRequests()
        servePerformers()
        // serveGemini(Api.Gemini, app.gemini)
        serveSpeech(Api.Speech, app.speech)
        serveUserHub()
        servePosts()
        serveChat()
        serveMap()
    }
}