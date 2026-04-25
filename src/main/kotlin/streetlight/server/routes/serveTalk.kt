package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import kampfire.api.StringId
import kampfire.model.Ok
import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.postApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import streetlight.model.Api
import streetlight.model.data.GalaxyId
import streetlight.model.data.TalkMessage
import streetlight.model.data.TalkRequest
import streetlight.model.data.SpaceType
import streetlight.server.model.StreetlightRouting
import streetlight.server.model.server
import java.util.concurrent.ConcurrentHashMap

fun StreetlightRouting.serveTalk() {
    val dao = server.dao.talk

    getEndpoint(Api.Talk.ReadGalaxy, { GalaxyId(it) }) {
        dao.readGalaxyTalk(it.data)
    }

    val clientSpaces = ConcurrentHashMap<StringId, TalkSpace>()
    val spaceLocks = Mutex() // use a per-key lock if traffic is heavy

    authenticateJwt(optional = true) {

        webSocket(Api.Talk.Connect.path) {
            val identity = identity.getIdentityOrNull(call)
            val stringId = call.parameters["id"]
            val space = call.parameters["space"]?.let { SpaceType.from(it) }
            if (stringId == null || space == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@webSocket
            }

            val clientSpace = spaceLocks.withLock {
                clientSpaces.getOrPut(stringId) { TalkSpace(stringId, space, server) }
            }

            try {
                clientSpace.addClient(this)

                for (frame in incoming) {
                    if (frame is Frame.Binary) {
                        val bytes = frame.readBytes()
                        clientSpace.takeClientBytes(bytes, identity)
                    }
                }
            } finally {
                spaceLocks.withLock {
                    val isEmpty = clientSpace.removeClient(this)
                    if (isEmpty) {
                        clientSpaces.remove(stringId)
                    }
                }
            }
        }

        postApi(Api.Talk.CreateComment) {
            val identity = identity.getIdentityOrNull(call)
            val comment = it.data
            val commentId = dao.writeComment(comment, identity?.userId)
            spaceLocks.withLock {
                val space = clientSpaces[comment.spaceId] ?: return@withLock
                space.takeComment(commentId, comment, identity)
            }
            Ok(commentId)
        }
    }
}