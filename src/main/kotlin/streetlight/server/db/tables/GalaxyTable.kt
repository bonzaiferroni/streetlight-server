package streetlight.server.db.tables

import klutch.db.point
import klutch.utils.toGeoPoint
import klutch.utils.toPGpoint
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.Galaxy
import streetlight.server.utils.toProjectId

object GalaxyTable : UUIDTable("galaxy") {
    val name = text("name")
    val center = point("center")
}

fun ResultRow.toGalaxy() = Galaxy(
    galaxyId = toProjectId(GalaxyTable.id),
    name = this[GalaxyTable.name],
    center = this[GalaxyTable.center].toGeoPoint(),
)

fun UpdateBuilder<*>.writeFull(galaxy: Galaxy) {
    this[GalaxyTable.id] = galaxy.galaxyId.toUUID()
    writeUpdate(galaxy)
}

fun UpdateBuilder<*>.writeUpdate(galaxy: Galaxy) {
    this[GalaxyTable.name] = galaxy.name
    this[GalaxyTable.center] = galaxy.center.toPGpoint()
}
