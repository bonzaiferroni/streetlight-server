package streetlight.server.db.tables

import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.EventPostRow
import streetlight.server.utils.toProjectId

object EventPostTable : UUIDTable("event_post") {
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE)
    val eventId = reference("event_id", EventTable.id, onDelete = ReferenceOption.CASCADE).index() // td: allow null for deleted events
    val starId = reference("user_id", StarTable.id, onDelete = ReferenceOption.SET_NULL).index().nullable()
    val text = text("text").nullable()
    val updatedAt = timestamp("updated_at").index()
    val createdAt = timestamp("created_at").index()

    init {
        uniqueIndex("galaxy_event_index", galaxyId, eventId)
    }
}

fun ResultRow.toEventPostRow() = EventPostRow(
    postId = this[EventPostTable.id].toProjectId(),
    galaxyId = this[EventPostTable.galaxyId].toProjectId(),
    eventId = this[EventPostTable.eventId].toProjectId(),
    starId = this[EventPostTable.starId]?.toProjectId(),
    text = this[EventPostTable.text],
    updatedAt = this[EventPostTable.updatedAt],
    createdAt = this[EventPostTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(post: EventPostRow) {
    this[EventPostTable.id] = post.postId.toUUID()
    this[EventPostTable.galaxyId] = post.galaxyId.toUUID()
    this[EventPostTable.starId] = post.starId?.toUUID()
    this[EventPostTable.createdAt] = post.createdAt
    writeUpdate(post)
}

fun UpdateBuilder<*>.writeUpdate(post: EventPostRow) {
    this[EventPostTable.eventId] = post.eventId.toUUID()
    this[EventPostTable.text] = post.text
    this[EventPostTable.updatedAt] = post.updatedAt
}
