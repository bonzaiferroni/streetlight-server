package streetlight.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.server.db.services.EventInfoService
import streetlight.server.db.services.SongService
import streetlight.server.html.eventPage
import java.io.File

fun Application.configureHtmlRouting(host: String) {
    routing {
//        staticFiles("/static", File("www"))
        staticFiles("/", File("www/kvision"))

//        get("/") {
//            call.respondHtml(HttpStatusCode.OK) {
//                homePage()
//            }
//        }
//
//        get("/about") {
//            call.respondHtml(HttpStatusCode.OK) {
//                aboutPage()
//            }
//        }

        get("/qr") {
            val eventService = EventInfoService()
            val id = call.parameters["id"]?.toInt() ?: suspend {
                // return the last event
                val events = eventService.readAll()
                if (events.isEmpty()) {
                    null
                } else {
                    events.last().event.id
                }
            }()
            if (id == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val event = eventService.read(id)!!
            val songService = SongService()
            val songs = songService.readAll()
            call.respondHtml(HttpStatusCode.OK) {
                eventPage(host, event, songs)
            }
        }

//        get("map") {
//            val locations = LocationService().readAll()
//            call.respondHtml(HttpStatusCode.OK) {
//                mapPage(locations)
//            }
//        }
    }
}