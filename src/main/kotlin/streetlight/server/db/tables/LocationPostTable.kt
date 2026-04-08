package streetlight.server.db.tables

import klutch.db.tables.UserTable
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.LocationPostRow
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toUserIdOrNull

object LocationPostTable : UUIDTable("location_post") {
    val locationId = reference("location_id", LocationTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val userId = reference("user_id", UserTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val username = text("username").nullable()
    val title = text("title").nullable()
    val text = text("text").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

object GalaxyLocationPostTable: Table("galaxy_location_post") {
    val userId = reference("user_id", UserTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE)
    val postId = reference("post_id", LocationPostTable.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(galaxyId, postId)
}

fun ResultRow.toLocationPostRow() = LocationPostRow(
    postId = this[LocationPostTable.id].toProjectId(),
    locationId = this[LocationPostTable.locationId]?.toProjectId(),
    userId = toUserIdOrNull(LocationPostTable.userId),
    username = this[LocationPostTable.username],
    title = this[LocationPostTable.title],
    text = this[LocationPostTable.text],
    updatedAt = this[LocationPostTable.updatedAt],
    createdAt = this[LocationPostTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(post: LocationPostRow) {
    this[LocationPostTable.id] = post.postId.toUUID()
    this[LocationPostTable.userId] = post.userId?.toUUID()
    this[LocationPostTable.username] = post.username
    this[LocationPostTable.createdAt] = post.createdAt
    writeUpdate(post)
}

fun UpdateBuilder<*>.writeUpdate(post: LocationPostRow) {
    this[LocationPostTable.locationId] = post.locationId?.toUUID()
    this[LocationPostTable.title] = post.title
    this[LocationPostTable.text] = post.text
    this[LocationPostTable.updatedAt] = post.updatedAt
}