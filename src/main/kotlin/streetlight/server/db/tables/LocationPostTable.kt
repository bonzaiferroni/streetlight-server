package streetlight.server.db.tables

import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.EventPostRow
import streetlight.model.data.LocationPostRow
import streetlight.server.utils.toProjectId

object LocationPostTable : UUIDTable("location_post") {
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE)
    val locationId = reference("location_id", LocationTable.id, onDelete = ReferenceOption.CASCADE) // td: make nullable
    val username = text("username").nullable()
    val title = text("title")
    val text = text("text").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toLocationPostRow() = LocationPostRow(
    postId = this[LocationPostTable.id].toProjectId(),
    galaxyId = this[LocationPostTable.galaxyId].toProjectId(),
    locationId = this[LocationPostTable.locationId].toProjectId(),
    username = this[LocationPostTable.username],
    text = this[LocationPostTable.text],
    updatedAt = this[LocationPostTable.updatedAt],
    createdAt = this[LocationPostTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(post: LocationPostRow) {
    this[LocationPostTable.id] = post.postId.toUUID()
    this[LocationPostTable.galaxyId] = post.galaxyId.toUUID()
    this[LocationPostTable.createdAt] = post.createdAt
    writeUpdate(post)
}

fun UpdateBuilder<*>.writeUpdate(post: LocationPostRow) {
    this[LocationPostTable.locationId] = post.locationId.toUUID()
    this[LocationPostTable.username] = post.username
    this[LocationPostTable.text] = post.text
    this[LocationPostTable.updatedAt] = post.updatedAt
}