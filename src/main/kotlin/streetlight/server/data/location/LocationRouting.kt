package streetlight.server.data.location

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import streetlight.model.Location
import streetlight.server.data.getIdOrThrow

fun Routing.locationRouting(locationService: LocationService) {
    get("/locations") {
        val locations = locationService.readAll()
        call.respond(locations)
    }

    // search events
    get("/locations?search={search}&count={count}") {
        val search = call.parameters["search"] ?: ""
        val count = call.parameters["count"]?.toIntOrNull() ?: 10
        val locations = locationService.search(search, count)
        call.respond(HttpStatusCode.OK, locations)
    }

    post("/locations") {
        val location = call.receive<Location>()
        val id = locationService.create(location)
        call.respond(HttpStatusCode.Created, id)
    }

    // Read location
    get("/locations/{id}") {
        val id = call.getIdOrThrow()
        val location = locationService.read(id)
        if (location != null) {
            call.respond(HttpStatusCode.OK, location)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    // Update location
    put("/locations/{id}") {
        val id = call.getIdOrThrow()
        val location = call.receive<Location>()
        locationService.update(id, location)
        call.respond(HttpStatusCode.OK)
    }

    // Delete location
    delete("/locations/{id}") {
        val id = call.getIdOrThrow()
        locationService.delete(id)
        call.respond(HttpStatusCode.OK)
    }
}