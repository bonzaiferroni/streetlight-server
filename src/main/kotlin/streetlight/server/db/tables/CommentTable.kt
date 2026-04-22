package streetlight.server.db.tables

import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.CommentId
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toProjectIdOrNull
import kotlin.time.Instant

object CommentTable: UUIDTable("comment") {
    val parentId = reference("parent_id", CommentTable, onDelete = ReferenceOption.SET_NULL).index().nullable()
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val text = text("text")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at").index()

    val lightCount = CommentLightTable.starId.countDistinct()
    val reply = CommentTable.alias("reply_comment")
    val replyCount = reply[CommentTable.id].countDistinct()
}

object CommentLightTable: Table("comment_light") {
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val commentId = reference("comment_id", CommentTable, onDelete = ReferenceOption.CASCADE).index()

    override val primaryKey = PrimaryKey(starId, commentId)
}

object GalaxyCommentTable: Table("galaxy_comment") {
    val galaxyId = reference("galaxy_id", GalaxyTable, onDelete = ReferenceOption.CASCADE).index()
    val commentId = reference("comment_id", CommentTable, onDelete = ReferenceOption.CASCADE).index()

    override val primaryKey = PrimaryKey(galaxyId, commentId)
}

fun ResultRow.toComment() = CommentRow(
    commentId = toProjectId(CommentTable.id),
    parentId = toProjectIdOrNull(CommentTable.parentId),
    starId = toProjectId(CommentTable.starId),
    text = this[CommentTable.text],
    updatedAt = this[CommentTable.updatedAt],
    createdAt = this[CommentTable.createdAt],
)

fun UpdateBuilder<*>.writeFull(comment: CommentRow) {
    this[CommentTable.id] = comment.commentId.toUUID()
    this[CommentTable.parentId] = comment.parentId?.toUUID()
    this[CommentTable.starId] = comment.starId.toUUID()
    this[CommentTable.createdAt] = comment.createdAt
    writeUpdate(comment)
}

fun UpdateBuilder<*>.writeUpdate(comment: CommentRow) {
    this[CommentTable.text] = comment.text
    this[CommentTable.updatedAt] = comment.updatedAt
}

data class CommentRow(
    val commentId: CommentId,
    val parentId: CommentId?,
    val starId: CommentId,
    val text: String,
    val updatedAt: Instant,
    val createdAt: Instant,
)