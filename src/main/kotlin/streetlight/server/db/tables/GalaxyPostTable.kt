package streetlight.server.db.tables

import klutch.db.point
import klutch.utils.toGeoPoint
import klutch.utils.toPGpoint
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.GalaxyPostRow
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toProjectIdOrNull

object GalaxyPostTable : UUIDTable("galaxy_post") {
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE)
    val username = text("username").nullable()
    val eventId = reference("event_id", EventTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val locationId = reference("location_id", LocationTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val title = text("title").nullable()
    val text = text("text").nullable()
    val geoPoint = point("geo_point").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toGalaxyPostRow() = GalaxyPostRow(
    galaxyPostId = toProjectId(GalaxyPostTable.id),
    galaxyId = toProjectId(GalaxyPostTable.galaxyId),
    username = this[GalaxyPostTable.username],
    eventId = toProjectIdOrNull(GalaxyPostTable.eventId),
    locationId = toProjectIdOrNull(GalaxyPostTable.locationId),
    title = this[GalaxyPostTable.title],
    text = this[GalaxyPostTable.text],
    geoPoint = this[GalaxyPostTable.geoPoint]?.toGeoPoint(),
    updatedAt = this[GalaxyPostTable.updatedAt],
    createdAt = this[GalaxyPostTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(galaxyPost: GalaxyPostRow) {
    this[GalaxyPostTable.id] = galaxyPost.galaxyPostId.toUUID()
    this[GalaxyPostTable.galaxyId] = galaxyPost.galaxyId.toUUID()
    this[GalaxyPostTable.createdAt] = galaxyPost.createdAt
    writeUpdate(galaxyPost)
}

fun UpdateBuilder<*>.writeUpdate(galaxyPost: GalaxyPostRow) {
    this[GalaxyPostTable.username] = galaxyPost.username
    this[GalaxyPostTable.eventId] = galaxyPost.eventId?.toUUID()
    this[GalaxyPostTable.locationId] = galaxyPost.locationId?.toUUID()
    this[GalaxyPostTable.title] = galaxyPost.title
    this[GalaxyPostTable.text] = galaxyPost.text
    galaxyPost.geoPoint?.let {
        this[GalaxyPostTable.geoPoint] = it.toPGpoint()
    }

    this[GalaxyPostTable.updatedAt] = galaxyPost.updatedAt
}
