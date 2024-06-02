package streetlight.server.data.user

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
import streetlight.model.Performance
import streetlight.server.data.getIdOrThrow
import streetlight.server.plugins.v1

fun Routing.performanceRouting(performanceService: PerformanceService) {

    // Fetch all performances
    get("$v1/performances") {
        val performances = performanceService.readAll()
        call.respond(HttpStatusCode.OK, performances)
    }

    // Read performance
    get("$v1/performances/{id}") {
        val id = call.getIdOrThrow()
        val performance = performanceService.read(id)
        if (performance != null) {
            call.respond(HttpStatusCode.OK, performance)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    authenticate("auth-jwt") {
        post("$v1/performances") {
            val performance = call.receive<Performance>()
            val id = performanceService.create(performance)
            call.respond(HttpStatusCode.Created, id)
        }

        // Update performance
        put("$v1/performances/{id}") {
            val id = call.getIdOrThrow()
            val performance = call.receive<Performance>()
            performanceService.update(id, performance)
            call.respond(HttpStatusCode.OK)
        }

        // Delete performance
        delete("$v1/performances/{id}") {
            val id = call.getIdOrThrow()
            performanceService.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}