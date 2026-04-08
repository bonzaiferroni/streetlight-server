package streetlight.server.db.tables

import klutch.db.tables.BasicUserTable
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.EventPostRow
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toUserIdOrNull

object EventPostTable : UUIDTable("event_post") {
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE)
    val eventId = reference("event_id", EventTable.id, onDelete = ReferenceOption.CASCADE) // td: allow null for deleted events
    val userId = reference("user_id", BasicUserTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val username = text("username").nullable()
    val text = text("text").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toEventPostRow() = EventPostRow(
    postId = this[EventPostTable.id].toProjectId(),
    galaxyId = this[EventPostTable.galaxyId].toProjectId(),
    eventId = this[EventPostTable.eventId].toProjectId(),
    userId = toUserIdOrNull(EventPostTable.userId),
    username = this[EventPostTable.username],
    text = this[EventPostTable.text],
    updatedAt = this[EventPostTable.updatedAt],
    createdAt = this[EventPostTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(post: EventPostRow) {
    this[EventPostTable.id] = post.postId.toUUID()
    this[EventPostTable.galaxyId] = post.galaxyId.toUUID()
    this[EventPostTable.userId] = post.userId?.toUUID()
    this[EventPostTable.username] = post.username
    this[EventPostTable.createdAt] = post.createdAt
    writeUpdate(post)
}

fun UpdateBuilder<*>.writeUpdate(post: EventPostRow) {
    this[EventPostTable.eventId] = post.eventId.toUUID()
    this[EventPostTable.text] = post.text
    this[EventPostTable.updatedAt] = post.updatedAt
}
