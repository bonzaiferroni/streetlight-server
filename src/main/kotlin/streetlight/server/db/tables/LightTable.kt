package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object EventLightTable: Table("event_light") {
    val eventId = reference("event_id", EventTable, onDelete = ReferenceOption.CASCADE).index()
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(eventId, starId)
}

object GalaxyLightTable : Table("galaxy_light") {
    // experimenting with pascal case columns
    val galaxyId = reference("galaxy_id", GalaxyTable, onDelete = ReferenceOption.CASCADE)
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(galaxyId, starId)
}