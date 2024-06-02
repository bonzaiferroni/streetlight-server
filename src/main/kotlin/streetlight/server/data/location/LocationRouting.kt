package streetlight.server.data.location

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
import streetlight.model.Location
import streetlight.server.data.getIdOrThrow
import streetlight.server.plugins.v1

fun Routing.locationRouting(locationService: LocationService) {
    get("$v1/locations") {
        val search = call.parameters["search"] ?: ""
        val count = call.parameters["limit"]?.toIntOrNull() ?: 10
        val locations = if (search.isBlank()) {
            locationService.readAll()
        } else {
            locationService.search(search, count)
        }
        call.respond(locations)
    }

    // Read location
    get("$v1/locations/{id}") {
        val id = call.getIdOrThrow()
        val location = locationService.read(id)
        if (location != null) {
            call.respond(HttpStatusCode.OK, location)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    authenticate("auth-jwt") {
        post("$v1/locations") {
            val location = call.receive<Location>()
            val id = locationService.create(location)
            call.respond(HttpStatusCode.Created, id)
        }

        // Update location
        put("$v1/locations/{id}") {
            val id = call.getIdOrThrow()
            val location = call.receive<Location>()
            locationService.update(id, location)
            call.respond(HttpStatusCode.OK)
        }

        // Delete location
        delete("$v1/locations/{id}") {
            val id = call.getIdOrThrow()
            locationService.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}