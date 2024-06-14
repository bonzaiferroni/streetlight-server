package streetlight.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticFiles
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import streetlight.server.data.event.EventInfoService
import streetlight.server.data.location.LocationEntity
import streetlight.server.data.location.LocationService
import streetlight.server.data.user.PerformanceService
import streetlight.server.html.*
import java.io.File

fun Application.configureHtmlRouting() {
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

        get("/request") {
            val performanceService = PerformanceService()
            val performances = performanceService.readAll()
            call.respondHtml(HttpStatusCode.OK) {
                requestPage(performances)
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
