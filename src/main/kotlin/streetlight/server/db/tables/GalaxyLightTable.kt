package streetlight.server.db.tables

import klutch.db.tables.BasicUserTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object GalaxyLightTable : Table("galaxy_light") {
    // experimenting with pascal case columns
    val GalaxyId = reference("event_id", GalaxyTable, onDelete = ReferenceOption.CASCADE)
    val UserId = reference("user_id", BasicUserTable, onDelete = ReferenceOption.CASCADE)
    val CreatedAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(GalaxyId, UserId)
}