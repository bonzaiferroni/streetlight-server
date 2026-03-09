package streetlight.server.db.tables

import klutch.db.point
import klutch.utils.toGeoPoint
import klutch.utils.toPGpoint
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.Galaxy
import streetlight.server.db.tables.TransitRouteStopTable.transitRouteId
import streetlight.server.db.tables.TransitRouteStopTable.transitStopId
import streetlight.server.utils.toProjectId

object GalaxyTable : UUIDTable("galaxy") {
    val pathId = text("path_id").uniqueIndex()
    val name = text("name")
    val center = point("center")
}

object GalaxyLocationTable: Table("galaxy_location") {
    val galaxyId = reference("galaxy_id", GalaxyTable.id, onDelete = ReferenceOption.CASCADE)
    val locationId = reference("location_id", LocationTable.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(galaxyId, locationId)
}

fun ResultRow.toGalaxy() = Galaxy(
    galaxyId = toProjectId(GalaxyTable.id),
    pathId = this[GalaxyTable.pathId],
    name = this[GalaxyTable.name],
    center = this[GalaxyTable.center].toGeoPoint(),
)

fun UpdateBuilder<*>.writeGalaxyFull(galaxy: Galaxy) {
    this[GalaxyTable.id] = galaxy.galaxyId.toUUID()
    writeGalaxyUpdate(galaxy)
}

fun UpdateBuilder<*>.writeGalaxyUpdate(galaxy: Galaxy) {
    this[GalaxyTable.name] = galaxy.name
    this[GalaxyTable.pathId] = galaxy.pathId
    this[GalaxyTable.center] = galaxy.center.toPGpoint()
}
