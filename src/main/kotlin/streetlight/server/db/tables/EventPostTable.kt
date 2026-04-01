package streetlight.server.db.tables

import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.EventPostRow
import streetlight.server.utils.toProjectId

object EventPostTable : UUIDTable("event_post") {
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE)
    val eventId = reference("event_id", EventTable.id, onDelete = ReferenceOption.CASCADE) // td: make nullable
    val username = text("username").nullable()
    val title = text("title")
    val text = text("text").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toEventPostRow() = EventPostRow(
    postId = this[EventPostTable.id].toProjectId(),
    galaxyId = this[EventPostTable.galaxyId].toProjectId(),
    username = this[EventPostTable.username],
    eventId = this[EventPostTable.eventId].toProjectId(),
    text = this[EventPostTable.text],
    updatedAt = this[EventPostTable.updatedAt],
    createdAt = this[EventPostTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(post: EventPostRow) {
    this[EventPostTable.id] = post.postId.toUUID()
    this[EventPostTable.galaxyId] = post.galaxyId.toUUID()
    this[EventPostTable.createdAt] = post.createdAt
    writeUpdate(post)
}

fun UpdateBuilder<*>.writeUpdate(post: EventPostRow) {
    this[EventPostTable.username] = post.username
    this[EventPostTable.eventId] = post.eventId.toUUID()
    this[EventPostTable.text] = post.text
    this[EventPostTable.updatedAt] = post.updatedAt
}
