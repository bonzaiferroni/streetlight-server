package streetlight.server.routes

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.send
import kampfire.api.StringId
import kampfire.model.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import streetlight.model.data.Comment
import streetlight.model.data.CommentId
import streetlight.model.data.NewComment
import streetlight.model.data.TalkComment
import streetlight.model.data.TalkHistory
import streetlight.model.data.TalkMessage
import streetlight.model.data.TalkRequest
import streetlight.model.data.SpaceType
import streetlight.model.data.StarId
import streetlight.server.model.StarIdentity
import streetlight.server.model.StreetlightServer
import java.util.Collections
import kotlin.collections.plusAssign
import kotlin.time.Clock

class TalkSpace(
    val spaceId: StringId,
    val space: SpaceType,
    val server: StreetlightServer
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val dao = server.dao.talk

    private val thumbCache = mutableMapOf<StarId, Url?>()

    private val clients = Collections.synchronizedSet<DefaultWebSocketServerSession>(
        LinkedHashSet()
    )

    suspend fun init() {
        // get talk space config
    }

    suspend fun readHistory() = dao.readComments(spaceId, space)

    suspend fun addClient(client: DefaultWebSocketServerSession) {
        clients += client
        val history = readHistory()
        client.send(TalkHistory(history).encode())
    }

    fun removeClient(client: DefaultWebSocketServerSession): Boolean {
        clients -= client
        return clients.isEmpty()
    }

    suspend fun takeClientBytes(bytes: ByteArray, identity: StarIdentity?) {
        // when (val request = bytes.decode()) {
        //     is SendComment -> takeComment(request, identity)
        // }
    }

    suspend fun takeComment(commentId: CommentId, comment: NewComment, identity: StarIdentity?) {
        val comment = Comment(
            commentId = commentId,
            parentId = comment.parentId,
            username = identity?.username,
            thumb = identity?.userId?.let { readThumb(it) },
            text = comment.text,
            lightCount = 0,
            replyCount = 0,
            updatedAt = Clock.System.now(),
            createdAt = Clock.System.now()
        )
        val message = TalkComment(comment)
        clients.forEach { client ->
            client.send(message.encode())
        }
    }

    private suspend fun readThumb(starId: StarId) =
        thumbCache[starId] ?: server.dao.star.readThumb(starId).also { thumbCache[starId] = it }
}

@OptIn(ExperimentalSerializationApi::class)
private fun ByteArray.decode(): TalkRequest = defaultCbor.decodeFromByteArray(serializer(), this)

@OptIn(ExperimentalSerializationApi::class)
private fun TalkMessage.encode(): ByteArray = defaultCbor.encodeToByteArray(serializer(), this)