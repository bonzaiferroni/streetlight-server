package streetlight.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import streetlight.server.model.ServerRouting
import streetlight.server.model.ServerScope
import streetlight.server.routes.*

fun Application.serveApi(server: ServerScope) {
    routing {
        val scope = ServerRouting(server, this)
        with (scope) {
            serveSession()
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
            serveMedia()
            serveFeedback()
        }
    }
}