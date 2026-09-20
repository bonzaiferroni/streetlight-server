package streetlight.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kabinet.utils.Environment
import klutch.server.provide
import streetlight.model.Api
import streetlight.server.ServerResource
import streetlight.server.buildMode
import streetlight.server.model.ServerRouting
import streetlight.server.model.ServerScope
import streetlight.server.routes.*

fun Application.serveApi(server: ServerScope) {
    install(IgnoreTrailingSlash)

    routing {
        val scope = ServerRouting(server, this)
        with (scope) {
            val resource = ServerResource(provide<Environment>().buildMode)
            servePages(resource)
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
            serveBug()
            serveSiteStatus()
            serveWebhooks()
            serveAccountActions()
            serveSubdomains(resource)
            serveMessages()
            serveSystem()
        }

        get("${Api.path}/{...}") {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}