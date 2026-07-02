package streetlight.server.db.tables

import kampfire.api.Markdown
import klutch.utils.transformMarkdown
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.CommentId
import streetlight.model.data.StarId
import streetlight.server.utils.toRecordId
import streetlight.server.utils.toRecordIdOrNull
import kotlin.time.Instant

object CommentTable: UuidTable("comment") {
    val parentId = reference("parent_id", CommentTable, ReferenceOption.SET_NULL).index().nullable()
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE).nullable().index()
    val text = text("text").transformMarkdown()
    val starCount = integer("star_count").default(0)
    val replyCount = integer("reply_count").default(0)
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at").index()
}

object GalaxyCommentTable: Table("galaxy_comment") {
    val galaxyId = reference("galaxy_id", GalaxyTable, ReferenceOption.CASCADE).index()
    val commentId = reference("comment_id", CommentTable, ReferenceOption.CASCADE).index()

    override val primaryKey = PrimaryKey(galaxyId, commentId)
}

object MediumCommentTable: Table("medium_comment") {
    val mediaId = reference("medium_id", MediumTable, ReferenceOption.CASCADE).index()
    val commentId = reference("comment_id", CommentTable, ReferenceOption.CASCADE).index()

    override val primaryKey = PrimaryKey(mediaId, commentId)
}

fun ResultRow.toCommentRow() = CommentRow(
    commentId = toRecordId(CommentTable.id),
    parentId = toRecordIdOrNull(CommentTable.parentId),
    starId = toRecordIdOrNull(CommentTable.starId),
    text = this[CommentTable.text],
    updatedAt = this[CommentTable.updatedAt],
    createdAt = this[CommentTable.createdAt],
)

fun UpdateBuilder<*>.createRecord(comment: CommentRow) {
    this[CommentTable.id] = comment.commentId.value
    this[CommentTable.parentId] = comment.parentId?.value
    this[CommentTable.starId] = comment.starId?.value
    this[CommentTable.createdAt] = comment.createdAt
    updateRecord(comment)
}

fun UpdateBuilder<*>.updateRecord(comment: CommentRow) {
    this[CommentTable.text] = comment.text
    this[CommentTable.updatedAt] = comment.updatedAt
}

data class CommentRow(
    val commentId: CommentId,
    val parentId: CommentId?,
    val starId: StarId?,
    val text: Markdown,
    val updatedAt: Instant,
    val createdAt: Instant,
)