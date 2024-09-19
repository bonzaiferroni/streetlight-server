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
import streetlight.server.extensions.testRole
import streetlight.server.plugins.Log
import streetlight.server.plugins.ROLE_ADMIN
import streetlight.server.plugins.authenticateJwt

inline fun <reified Data : IdModel, DataEntity : IntEntity> Routing.defaultRouting(
    endpoint: Endpoint, service: DataService<Data, DataEntity>
) {
    applyGet(endpoint, service)
    applyGetAll(endpoint, service)
    authenticateJwt {
        applyPost(endpoint, service)
        applyPut(endpoint, service)
        applyDelete(endpoint, service)
    }
}

inline fun <reified Data : IdModel, DataEntity : IntEntity> Routing.applyGet(
    endpoint: Endpoint,
    service: DataService<Data, DataEntity>
) {
    get(endpoint.serverIdTemplate) {
        Log.logDebug("Routing: GET ${endpoint.serverIdTemplate}")
        val id = call.getIdOrThrow()
        val data = service.read(id)
        if (data != null) {
            call.respond(HttpStatusCode.OK, data)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}

inline fun <reified Data : IdModel, DataEntity : IntEntity> Routing.applyGetAll(
    endpoint: Endpoint,
    service: DataService<Data, DataEntity>
) {
    get(endpoint.path) {
        Log.logDebug("Routing: GET ${endpoint.path}")
        val search = call.parameters["search"] ?: ""
        val count = call.parameters["limit"]?.toIntOrNull() ?: 10
        val data = if (search.isBlank()) {
            service.readAll()
        } else {
            service.search(service.getSearchOp(search), count)
        }
        call.respond(HttpStatusCode.OK, data)
    }
}

inline fun <reified Data : IdModel, DataEntity : IntEntity> Route.applyPost(
    endpoint: Endpoint,
    service: DataService<Data, DataEntity>
) {
    post(endpoint.path) {
        Log.logDebug("Routing: POST ${endpoint.path}")
        if (!call.testRole(ROLE_ADMIN)) {
            call.respond(HttpStatusCode.Forbidden)
            return@post
        }
        val data = call.receive<Data>()
        val id = service.create(data)
        if (id == -1) {
            Log.logInfo("Routing: POST unable to create data: ${endpoint.path}")
        }
        call.respond(HttpStatusCode.Created, id)
    }
}

inline fun <reified Data : IdModel, DataEntity : IntEntity> Route.applyPut(
    endpoint: Endpoint,
    service: DataService<Data, DataEntity>
) {
    put(endpoint.path) {
        Log.logDebug("Routing: PUT ${endpoint.path}")
        if (!call.testRole(ROLE_ADMIN)) {
            call.respond(HttpStatusCode.Forbidden)
            return@put
        }
        val data = call.receive<Data>()
        service.update(data)
        call.respond(HttpStatusCode.OK, true)
    }
}

inline fun <reified Data : IdModel, DataEntity : IntEntity> Route.applyDelete(
    endpoint: Endpoint,
    service: DataService<Data, DataEntity>
) {
    delete(endpoint.serverIdTemplate) {
        Log.logDebug("Routing: DELETE ${endpoint.serverIdTemplate}")
        if (!call.testRole(ROLE_ADMIN)) {
            call.respond(HttpStatusCode.Forbidden)
            return@delete
        }
        val id = call.getIdOrThrow()
        service.delete(id)
        call.respond(HttpStatusCode.OK, true)
    }
}