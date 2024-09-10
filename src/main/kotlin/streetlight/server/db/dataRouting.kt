package streetlight.server.db

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.IntEntity
import streetlight.model.Endpoint
import streetlight.model.core.IdModel
import streetlight.server.extensions.getIdOrThrow
import streetlight.server.plugins.ROLE_ADMIN
import streetlight.server.plugins.authenticateJwt
import streetlight.server.plugins.testRole

inline fun <reified Data : IdModel, DataEntity : IntEntity> Routing.dataRouting(
    endpoint: Endpoint, service: DataService<Data, DataEntity>
) {
    get(endpoint.path) {
        val search = call.parameters["search"] ?: ""
        val count = call.parameters["limit"]?.toIntOrNull() ?: 10
        val data = if (search.isBlank()) {
            service.readAll()
        } else {
            service.search(service.getSearchOp(search), count)
        }
        call.respond(HttpStatusCode.OK, data)
    }

    get(endpoint.serverIdTemplate) {
        val id = call.getIdOrThrow()
        val data = service.read(id)
        if (data != null) {
            call.respond(HttpStatusCode.OK, data)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    authenticateJwt {
        post(endpoint.path) {
            if (!call.testRole(ROLE_ADMIN)) {
                call.respond(HttpStatusCode.Forbidden)
                return@post
            }
            val data = call.receive<Data>()
            val id = service.create(data)
            call.respond(HttpStatusCode.Created, id)
        }

        put(endpoint.path) {
            if (!call.testRole(ROLE_ADMIN)) {
                call.respond(HttpStatusCode.Forbidden)
                return@put
            }
            val data = call.receive<Data>()
            service.update(data)
            call.respond(HttpStatusCode.OK, true)
        }

        delete(endpoint.serverIdTemplate) {
            if (!call.testRole(ROLE_ADMIN)) {
                call.respond(HttpStatusCode.Forbidden)
                return@delete
            }
            val id = call.getIdOrThrow()
            service.delete(id)
            call.respond(HttpStatusCode.OK, true)
        }
    }
}