package streetlight.server.db.tables

import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.LocationPostRow
import streetlight.server.db.tables.EventPostTable.eventId
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toProjectIdOrNull

object LocationPostTable : UUIDTable("location_post") {
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE).index()
    val locationId = reference("location_id", LocationTable.id, onDelete = ReferenceOption.SET_NULL).index().nullable()
    val starId = reference("star_id", StarTable.id, onDelete = ReferenceOption.SET_NULL).index().nullable()
    val text = text("text").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at").index()

    init {
        uniqueIndex("galaxy_location_index", galaxyId, locationId)
    }
}

fun ResultRow.toLocationPostRow() = LocationPostRow(
    postId = this[LocationPostTable.id].toProjectId(),
    locationId = this[LocationPostTable.locationId]?.toProjectId(),
    starId = toProjectIdOrNull(LocationPostTable.starId),
    text = this[LocationPostTable.text],
    updatedAt = this[LocationPostTable.updatedAt],
    createdAt = this[LocationPostTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(post: LocationPostRow) {
    this[LocationPostTable.id] = post.postId.toUUID()
    this[LocationPostTable.starId] = post.starId?.toUUID()
    this[LocationPostTable.locationId] = post.locationId?.toUUID()
    this[LocationPostTable.createdAt] = post.createdAt
    writeUpdate(post)
}

fun UpdateBuilder<*>.writeUpdate(post: LocationPostRow) {
    this[LocationPostTable.text] = post.text
    this[LocationPostTable.updatedAt] = post.updatedAt
}