package streetlight.server.db.tables

import kabinet.utils.toInstantFromUtc
import kabinet.utils.toLocalDateTimeUtc
import klutch.db.point
import klutch.utils.toGeoPoint
import klutch.utils.toPGpoint
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.Galaxy
import streetlight.server.db.tables.TransitRouteStopTable.transitRouteId
import streetlight.server.db.tables.TransitRouteStopTable.transitStopId
import streetlight.server.utils.toProjectId

object GalaxyTable : UUIDTable("galaxy") {
    val pathId = text("path_id").uniqueIndex()
    val name = text("name")
    val description = text("description")
    val center = point("center")
    val imageUrl = text("image_url").nullable()
    val thumbUrl = text("thumb_url").nullable()
    val updatedAt = datetime("updated_at")
    val createdAt = datetime("created_at")
}

object GalaxyLocationTable: Table("galaxy_location") {
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE)
    val locationId = reference("location_id", LocationTable.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(galaxyId, locationId)
}

object GalaxyEventTable: Table("galaxy_event") {
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE)
    val eventId = reference("event_id", EventTable.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(galaxyId, eventId)
}

fun ResultRow.toGalaxy() = Galaxy(
    galaxyId = toProjectId(GalaxyTable.id),
    pathId = this[GalaxyTable.pathId],
    name = this[GalaxyTable.name],
    description = this[GalaxyTable.description],
    center = this[GalaxyTable.center].toGeoPoint(),
    imageUrl = this[GalaxyTable.imageUrl],
    thumbUrl = this[GalaxyTable.thumbUrl],
    updatedAt = this[GalaxyTable.updatedAt].toInstantFromUtc(),
    createdAt = this[GalaxyTable.createdAt].toInstantFromUtc(),
)

fun UpdateBuilder<*>.writeGalaxyFull(galaxy: Galaxy) {
    this[GalaxyTable.id] = galaxy.galaxyId.toUUID()
    this[GalaxyTable.createdAt] = galaxy.createdAt.toLocalDateTimeUtc()
    writeGalaxyUpdate(galaxy)
}

fun UpdateBuilder<*>.writeGalaxyUpdate(galaxy: Galaxy) {
    this[GalaxyTable.name] = galaxy.name
    this[GalaxyTable.description] = galaxy.description
    this[GalaxyTable.pathId] = galaxy.pathId
    this[GalaxyTable.center] = galaxy.center.toPGpoint()
    this[GalaxyTable.imageUrl] = galaxy.imageUrl
    this[GalaxyTable.thumbUrl] = galaxy.thumbUrl
    this[GalaxyTable.updatedAt] = galaxy.updatedAt.toLocalDateTimeUtc()
}
