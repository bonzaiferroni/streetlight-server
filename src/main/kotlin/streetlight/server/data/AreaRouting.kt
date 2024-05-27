package streetlight.server.data

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import streetlight.model.Area

fun Routing.areaRouting(areaService: AreaService) {

    // Fetch all areas
    get("/areas") {
        val areas = areaService.readAll()
        call.respond(HttpStatusCode.OK, areas)
    }

    post("/areas") {
        val area = call.receive<Area>()
        val id = areaService.create(area)
        call.respond(HttpStatusCode.Created, id)
    }

    // Read area
    get("/areas/{id}") {
        val id = call.getIdOrThrow()
        val area = areaService.read(id)
        if (area != null) {
            call.respond(HttpStatusCode.OK, area)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    // Update area
    put("/areas/{id}") {
        val id = call.getIdOrThrow()
        val area = call.receive<Area>()
        areaService.update(id, area)
        call.respond(HttpStatusCode.OK)
    }

    // Delete area
    delete("/areas/{id}") {
        val id = call.getIdOrThrow()
        areaService.delete(id)
        call.respond(HttpStatusCode.OK)
    }
}