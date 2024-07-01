package streetlight.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import streetlight.server.data.services.EventInfoService
import streetlight.server.data.services.LocationService
import streetlight.server.data.services.PerformanceService
import streetlight.server.html.*
import java.io.File

fun Application.configureHtmlRouting(host: String) {
    routing {
        staticFiles("/static", File("www"))

        get("/") {
            call.respondHtml(HttpStatusCode.OK) {
                homePage()
            }
        }
        get("/events") {
            val eventInfoService = EventInfoService()
            val events = eventInfoService.readAll()
            call.respondHtml(HttpStatusCode.OK) {
                eventsPage(events)
            }
        }

        get("/event") {
            val id = call.parameters["id"]?.toInt() ?: suspend {
                // return the last event
                val eventService = EventInfoService()
                val events = eventService.readAll()
                if (events.isEmpty()) {
                    null
                } else {
                    events.last().id
                }
            }()
            if (id == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val performanceService = PerformanceService()

            val performances = performanceService.readAll()
            call.respondHtml(HttpStatusCode.OK) {
                eventPage(host, id, performances)
            }
        }

        get("map") {
            val locations = LocationService().readAll()
            call.respondHtml(HttpStatusCode.OK) {
                mapPage(locations)
            }
        }
    }
}
