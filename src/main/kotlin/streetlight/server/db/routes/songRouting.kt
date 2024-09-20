package streetlight.server.db.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.model.Endpoint
import streetlight.model.core.Song
import streetlight.server.db.services.SongService
import streetlight.server.extensions.getIdOrThrow
import streetlight.server.extensions.getUsername
import streetlight.server.plugins.Log
import streetlight.server.plugins.authenticateJwt

fun Routing.songRouting(endpoint: Endpoint) {
    val service = SongService()
    authenticateJwt {
        get(endpoint.path) {
            Log.logDebug("Routing: GET ${endpoint.path}")
            val username = call.getUsername()
            val songs = service.readSongs(username)
            call.respond(HttpStatusCode.OK, songs)
        }

        get(endpoint.serverIdTemplate) {
            Log.logDebug("Routing: GET ${endpoint.serverIdTemplate}")
            val username = call.getUsername()
            val id = call.getIdOrThrow()
            val song = service.readSong(id, username)
            call.respond(HttpStatusCode.OK, song)
        }

        put(endpoint.path) {
            Log.logDebug("Routing: PUT ${endpoint.path}")
            val username = call.getUsername()
            val song = call.receive<Song>()
            val data = service.update(song, username)
            call.respond(HttpStatusCode.OK, data)
        }

        post(endpoint.path) {
            Log.logDebug("Routing: POST ${endpoint.path}")
            val username = call.getUsername()
            val song = call.receive<Song>()
            val data = service.create(song, username)
            call.respond(HttpStatusCode.Created, data)
        }

        delete(endpoint.path) {
            Log.logDebug("Routing: DELETE ${endpoint.path}")
            val username = call.getUsername()
            val song = call.receive<Song>()
            service.delete(song, username)
            call.respond(HttpStatusCode.OK, true)
        }
    }
}