package streetlight.server.db

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import org.jetbrains.exposed.dao.IntEntity
import streetlight.server.plugins.*

inline fun <reified Data : Any, DataEntity : IntEntity> Routing.applyServiceRouting(
    endpoint: String, service: DataService<Data, DataEntity>
) {
    get("$v1/${endpoint}") {
        val search = call.parameters["search"] ?: ""
        val count = call.parameters["limit"]?.toIntOrNull() ?: 10
        val data = if (search.isBlank()) {
            service.readAll()
        } else {
            service.search(service.getSearchOp(search), count)
        }
        call.respond(HttpStatusCode.OK, data)
    }

    get("$v1/${endpoint}/{id}") {
        val id = call.getIdOrThrow()
        val data = service.read(id)
        if (data != null) {
            call.respond(HttpStatusCode.OK, data)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    authenticateJwt {
        post("$v1/${endpoint}") {
            if (!call.testRole(ROLE_ADMIN)) {
                call.respond(HttpStatusCode.Forbidden)
                return@post
            }
            val data = call.receive<Data>()
            val id = service.create(data)
            call.respond(HttpStatusCode.Created, id)
        }

        put("$v1/${endpoint}/{id}") {
            if (!call.testRole(ROLE_ADMIN)) {
                call.respond(HttpStatusCode.Forbidden)
                return@put
            }
            val id = call.getIdOrThrow()
            val data = call.receive<Data>()
            service.update(id, data)
            call.respond(HttpStatusCode.OK)
        }

        delete("$v1/${endpoint}/{id}") {
            if (!call.testRole(ROLE_ADMIN)) {
                call.respond(HttpStatusCode.Forbidden)
                return@delete
            }
            val id = call.getIdOrThrow()
            service.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}