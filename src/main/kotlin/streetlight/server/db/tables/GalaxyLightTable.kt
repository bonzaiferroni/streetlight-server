package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object GalaxyLightTable : Table("galaxy_light") {
    // experimenting with pascal case columns
    val GalaxyId = reference("event_id", GalaxyTable, onDelete = ReferenceOption.CASCADE)
    val StarId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE)
    val CreatedAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(GalaxyId, StarId)
}