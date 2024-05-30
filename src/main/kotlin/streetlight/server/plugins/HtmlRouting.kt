package streetlight.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import streetlight.server.data.event.EventInfoService
import streetlight.server.html.*

fun Application.configureHtmlRouting() {
    routing {
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
    }
}