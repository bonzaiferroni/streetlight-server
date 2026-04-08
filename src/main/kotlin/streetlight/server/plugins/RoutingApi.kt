package streetlight.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kampfire.model.BasicUser
import kampfire.model.UserId
import kampfire.model.provideBasicUser
import klutch.db.services.BasicUserTableDao
import klutch.gemini.serveSpeech
import klutch.server.*
import streetlight.model.Api
import streetlight.server.model.Streetlight
import streetlight.server.model.routingContextOf
import streetlight.server.routes.*
import kotlin.time.Clock

fun Application.configureApiRoutes(app: Streetlight) {
    routing {
        routingContextOf(app) {
            serveUsers(BasicUserTableDao(), ::provideBasicUser)
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