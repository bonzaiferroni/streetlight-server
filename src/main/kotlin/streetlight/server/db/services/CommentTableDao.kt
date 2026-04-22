package streetlight.server.db.services

import kabinet.console.globalConsole
import kampfire.model.thumb
import klutch.db.DbService
import klutch.db.readById
import klutch.utils.eq
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Comment
import streetlight.model.data.CommentId
import streetlight.model.data.GalaxyId
import streetlight.server.db.tables.CommentRow
import streetlight.server.db.tables.CommentTable
import streetlight.server.db.tables.GalaxyCommentTable
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toProjectIdOrNull

private val console = globalConsole.getHandle(CommentTableDao::class)

class CommentTableDao : DbService() {

    suspend fun create(comment: CommentRow) = dbQuery {
        CommentTable.insertAndGetId {
            it.writeFull(comment)
        }
        CommentTable.readById(comment.commentId.toUUID()).toComment()
    }

    suspend fun readComment(commentId: CommentId) = dbQuery {
        CommentTable.readById(commentId.toUUID()).toComment()
    }

    suspend fun readGalaxyComments(galaxyId: GalaxyId, limit: Int = 100) = dbQuery {
        GalaxyCommentQuery.where { GalaxyCommentTable.galaxyId.eq(galaxyId) }
            .orderBy(CommentTable.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toComment() }
    }

    suspend fun update(comment: CommentRow) = dbQuery {
        CommentTable.update({ CommentTable.id eq comment.commentId.toUUID() }) {
            it.writeUpdate(comment)
        }
        CommentTable.readById(comment.commentId.toUUID()).toComment()
    }

    suspend fun delete(commentId: CommentId) = dbQuery {
        CommentTable.deleteWhere { id eq commentId.toUUID() } > 0
    }
}

private val GalaxyCommentQuery get() = GalaxyCommentTable
    .join(CommentTable, JoinType.LEFT, GalaxyCommentTable.commentId, CommentTable.id)
    .toCommentQuery()

private fun Join.toCommentQuery() = join(StarTable, JoinType.LEFT, CommentTable.starId, StarTable.id)
    .select(CommentColumns)

private fun ResultRow.toComment() = Comment(
    commentId = toProjectId(CommentTable.id),
    parentId = toProjectIdOrNull(CommentTable.parentId),
    username = this[StarTable.username],
    thumb = this[StarTable.images].thumb,
    text = this[CommentTable.text],
    lightCount = this[CommentTable.lightCount],
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
    CommentTable.lightCount,
    StarTable.username,
    StarTable.images,
)