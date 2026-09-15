package streetlight.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import streetlight.model.Api
import streetlight.server.model.ServerRouting
import streetlight.server.model.ServerScope
import streetlight.server.routes.*

fun Application.serveApi(server: ServerScope) {
    install(IgnoreTrailingSlash)

    routing {
        val scope = ServerRouting(server, this)
        with (scope) {
            servePages()
            serveSession()
            serveEvents()
            serveGalaxies()
            serveStars()
            serveLocations()
            serveSongs()
            serveRenditions()
            serveGtfs()
            serveRequests()
            // serveGemini(Api.Gemini, app.gemini)
            // serveSpeech(Api.Speech, app.ai.speech)
            serveUserHub()
            serveChat()
            servePosts()
            serveSpiritVision()
            serveFiles()
            serveOmni()
            serveSiteDocs()
            serveTalk()
            serveContent()
            serveCity()
            serveTasks()
            serveMedia()
            serveFeedback()
            serveSiteStatus()
            serveWebhooks()
            serveAccountActions()
            serveSubdomains()
            serveMessages()
        }

        get("${Api.path}/{...}") {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}