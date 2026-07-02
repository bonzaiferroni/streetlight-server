package streetlight.server.db.services

import kabinet.console.globalConsole
import kampfire.api.toUsername
import kampfire.model.thumb
import klutch.db.DbService
import klutch.db.readById
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Comment
import streetlight.model.data.CommentId
import streetlight.model.data.GalaxyId
import streetlight.model.data.NewComment
import streetlight.model.data.PostId
import streetlight.model.data.StarId
import streetlight.model.data.SpaceType
import streetlight.model.data.UpdatedComment
import streetlight.server.db.tables.CommentRow
import streetlight.server.db.tables.CommentTable
import streetlight.server.db.tables.GalaxyCommentTable
import streetlight.server.db.tables.MediumCommentTable
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.updateRecord
import streetlight.server.utils.toRecordId
import streetlight.server.utils.toRecordIdOrNull
import kotlin.time.Clock
import kotlin.uuid.Uuid

private val console = globalConsole.getHandle(CommentTableDao::class)

class CommentTableDao : DbService() {

    suspend fun create(comment: CommentRow) = dbQuery {
        CommentTable.insertAndGetId {
            it.createRecord(comment)
        }
        CommentTable.readById(comment.commentId.value).toComment()
    }

    suspend fun readComment(commentId: CommentId) = dbQuery {
        CommentQuery.where { CommentTable.id.eq(commentId) }.firstOrNull()?.toComment()
    }

    suspend fun readComments(id: Uuid, space: SpaceType, limit: Int = 100) = when (space) {
        SpaceType.Galaxy -> readGalaxyTalk(GalaxyId(id), limit)
        SpaceType.Post -> readPostTalk(PostId(id), limit)
    }

    suspend fun writeComment(comment: NewComment, starId: StarId?) = when (comment.spaceType) {
        SpaceType.Galaxy -> writeGalaxyComment(comment, starId)
        SpaceType.Post -> writePostComment(comment, starId)
    }

    suspend fun writeGalaxyComment(comment: NewComment, starId: StarId?) = dbQuery {
        val commentId = insertComment(comment, starId)
        GalaxyCommentTable.insert {
            it[GalaxyCommentTable.galaxyId] = comment.galaxyId.value
            it[GalaxyCommentTable.commentId] = commentId.value
        }
        commentId
    }

    suspend fun writePostComment(comment: NewComment, starId: StarId?) = dbQuery {
        val commentId = insertComment(comment, starId)
        MediumCommentTable.insert {
            it[MediumCommentTable.mediaId] = comment.postId.value
            it[MediumCommentTable.commentId] = commentId.value
        }
        commentId
    }

    private fun insertComment(comment: NewComment, starId: StarId?): CommentId {
        val commentId = CommentId.random()
        CommentTable.insert {
            it.createRecord(CommentRow(
                commentId = commentId,
                parentId = comment.parentId,
                starId = starId,
                text = comment.text,
                updatedAt = Clock.System.now(),
                createdAt = Clock.System.now()
            ))
        }
        return commentId
    }

    suspend fun updateComment(comment: UpdatedComment, starId: StarId?) = dbQuery {
        CommentTable.update({ CommentTable.id.eq(comment.commentId) and CommentTable.starId.eq(starId?.value) }) {
            it[CommentTable.text] = comment.text
        } == 1
    }

    suspend fun readGalaxyTalk(galaxyId: GalaxyId, limit: Int = 100) = dbQuery {
        GalaxyCommentQuery.where { GalaxyCommentTable.galaxyId.eq(galaxyId) }
            .orderBy(CommentTable.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toComment() }
    }

    suspend fun readPostTalk(postId: PostId, limit: Int = 100) = dbQuery {
        PostCommentQuery.where { MediumCommentTable.mediaId.eq(postId) }
            .orderBy(CommentTable.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toComment() }
    }

    suspend fun update(comment: CommentRow) = dbQuery {
        CommentTable.update({ CommentTable.id eq comment.commentId.value }) {
            it.updateRecord(comment)
        }
        CommentTable.readById(comment.commentId.value).toComment()
    }

    suspend fun delete(commentId: CommentId) = dbQuery {
        CommentTable.deleteWhere { id eq commentId.value } > 0
    }
}

private val GalaxyCommentQuery get() = GalaxyCommentTable
    .join(CommentTable, JoinType.LEFT, GalaxyCommentTable.commentId, CommentTable.id)
    .toCommentQuery()

private val PostCommentQuery get() = MediumCommentTable
    .join(CommentTable, JoinType.LEFT, MediumCommentTable.commentId, CommentTable.id)
    .toCommentQuery()

private val CommentQuery get() = CommentTable.join(StarTable, JoinType.LEFT, CommentTable.starId, StarTable.id)
    .select(CommentColumns)

private fun Join.toCommentQuery() = join(StarTable, JoinType.LEFT, CommentTable.starId, StarTable.id)
    .select(CommentColumns)

private fun ResultRow.toComment() = Comment(
    commentId = toRecordId(CommentTable.id),
    parentId = toRecordIdOrNull(CommentTable.parentId),
    username = this[StarTable.username].toUsername(),
    thumb = this[StarTable.images].thumb,
    text = this[CommentTable.text],
    lightCount = this[CommentTable.starCount],
    replyCount = this[CommentTable.replyCount],
    updatedAt = this[CommentTable.updatedAt],
    createdAt = this[CommentTable.createdAt],
)

private val CommentColumns = listOf(
    CommentTable.id,
    CommentTable.parentId,
    CommentTable.text,
    CommentTable.updatedAt,
    CommentTable.createdAt,
    CommentTable.replyCount,
    CommentTable.starCount,
    StarTable.username,
    StarTable.images,
)