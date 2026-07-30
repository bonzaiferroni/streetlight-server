package streetlight.server.routes

import kampfire.model.Url
import klutch.db.model.Identity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import streetlight.model.data.Comment
import streetlight.model.data.CommentId
import streetlight.model.data.NewComment
import streetlight.model.data.CommentCreated
import streetlight.model.data.TalkMessage
import streetlight.model.data.SpaceType
import streetlight.model.data.StarId
import streetlight.model.data.CommentUpdated
import streetlight.model.data.UpdatedComment
import streetlight.server.model.DaoFacade
import streetlight.server.utils.starId
import kotlin.time.Clock
import kotlin.uuid.Uuid

class TalkSpace(
    val spaceId: Uuid,
    val space: SpaceType,
    private val dao: DaoFacade,
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val thumbCache = mutableMapOf<StarId, Url?>()
    private val messageSharedFlow = MutableSharedFlow<TalkMessage>()
    val messageFlow: Flow<TalkMessage> = messageSharedFlow

    var clientCount = 0
        private set

    suspend fun init() {
        // get talk space config
    }

    suspend fun addClient() {
        clientCount++
    }

    fun removeClient(): Boolean {
        return --clientCount == 0
    }

    suspend fun sendNewComment(commentId: CommentId, comment: NewComment, identity: Identity?) {
        val comment = Comment(
            commentId = commentId,
            parentId = comment.parentId,
            username = identity?.username,
            thumb = identity?.starId?.let { readThumb(it) },
            text = comment.text,
            lightCount = 0,
            replyCount = 0,
            updatedAt = Clock.System.now(),
            createdAt = Clock.System.now()
        )
        messageSharedFlow.emit(CommentCreated(comment))
    }

    suspend fun sendUpdatedComment(comment: UpdatedComment) {
        messageSharedFlow.emit(CommentUpdated(comment.commentId, comment.text))
    }

    private suspend fun readThumb(starId: StarId) =
        thumbCache[starId] ?: dao.star.readThumb(starId).also { thumbCache[starId] = it }
}
