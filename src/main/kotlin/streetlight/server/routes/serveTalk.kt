package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.sse.sse
import io.ktor.util.cio.ChannelWriteException
import kampfire.api.StringId
import kampfire.model.Ok
import klutch.server.ApiContext
import klutch.server.getApi
import klutch.server.getEndpoint
import klutch.server.postApi
import klutch.server.readParamOrNull
import koala.utils.jsonConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.serializer
import streetlight.model.Api
import streetlight.model.data.GalaxyId
import streetlight.model.data.SpaceType
import streetlight.model.data.TalkMessage
import streetlight.model.data.TalkRequest
import streetlight.server.model.dao
import streetlight.server.model.getIdentity
import streetlight.server.model.getIdentityOrNull
import klutch.server.authGate
import java.util.concurrent.ConcurrentHashMap

fun ApiContext.serveTalk() {

    getEndpoint(Api.Talk.ReadGalaxy, { GalaxyId(it) }) {
        dao.talk.readGalaxyTalk(it.data)
    }

    getApi(Api.Talk.ReadHistory) {
        val spaceId = readParamOrNull(it.spaceId) ?: return@getApi null
        val spaceType = readParamOrNull(it.spaceType) ?: return@getApi null
        Ok(dao.talk.readComments(spaceId, spaceType))
    }

    val clientSpaces = ConcurrentHashMap<StringId, TalkSpace>()
    val spaceLocks = Mutex() // use a per-key lock if traffic is heavy

    authGate(optional = true) {

        sse(Api.Talk.Connect.path) {
            // val identity = identity.getIdentityOrNull(call)
            val stringId = call.parameters["id"]
            val space = call.parameters["space"]?.let { SpaceType.from(it) }
            if (stringId == null || space == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@sse
            }

            val clientSpace = spaceLocks.withLock {
                clientSpaces.getOrPut(stringId) { TalkSpace(stringId, space, dao) }
            }

            try {
                clientSpace.addClient()
                clientSpace.messageFlow.collect { message ->
                    send(message.encode())
                }
            } catch (e: ChannelWriteException) {
                // client disconnected mid-write
            } finally {
                spaceLocks.withLock {
                    val isEmpty = clientSpace.removeClient()
                    if (isEmpty) {
                        clientSpaces.remove(stringId)
                    }
                }
            }
        }

        postApi(Api.Talk.CreateComment) {
            val identity = call.getIdentityOrNull()
            val comment = it.data
            val commentId = dao.talk.writeComment(comment, identity?.starId)
            // td: move off thread
            spaceLocks.withLock {
                val space = clientSpaces[comment.spaceId] ?: return@withLock
                space.sendNewComment(commentId, comment, identity)
            }
            Ok(commentId)
        }
    }

    authGate {
        postApi(Api.Talk.UpdateComment) {
            val identity = call.getIdentity()
            val comment = it.data
            val result = dao.talk.updateComment(comment, identity.starId)
            // td: move off thread
            spaceLocks.withLock {
                val space = clientSpaces[comment.spaceId] ?: return@withLock
                space.sendUpdatedComment(comment)
            }

            Ok(result)
        }
    }
}

private fun String.decode(): TalkRequest = jsonConfig.decodeFromString(serializer(), this)
private fun TalkMessage.encode(): String = jsonConfig.encodeToString(serializer(), this)