package streetlight.server.data.area

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import streetlight.model.Area
import streetlight.server.data.getIdOrThrow
import streetlight.server.plugins.v1

fun Routing.areaRouting(areaService: AreaService) {

    // Fetch all areas
    get("$v1/areas") {
        val areas = areaService.readAll()
        call.respond(HttpStatusCode.OK, areas)
    }

    // Read area
    get("$v1/areas/{id}") {
        val id = call.getIdOrThrow()
        val area = areaService.read(id)
        if (area != null) {
            call.respond(HttpStatusCode.OK, area)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    authenticate("auth-jwt") {
        post("$v1/areas") {
            val area = call.receive<Area>()
            val id = areaService.create(area)
            call.respond(HttpStatusCode.Created, id)
        }

        // Update area
        put("$v1/areas/{id}") {
            val id = call.getIdOrThrow()
            val area = call.receive<Area>()
            areaService.update(id, area)
            call.respond(HttpStatusCode.OK)
        }

        // Delete area
        delete("$v1/areas/{id}") {
            val id = call.getIdOrThrow()
            areaService.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}