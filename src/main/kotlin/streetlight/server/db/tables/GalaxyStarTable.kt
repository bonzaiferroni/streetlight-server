package streetlight.server.db.tables

import klutch.db.tables.UserTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object GalaxyStarTable : Table("galaxy_star") {
    val GalaxyId = reference("event_id", GalaxyTable, onDelete = ReferenceOption.CASCADE)
    val UserId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val CreatedAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(GalaxyId, UserId)
}