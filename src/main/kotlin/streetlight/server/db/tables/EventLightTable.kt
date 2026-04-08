package streetlight.server.db.tables

import klutch.db.tables.UserTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.server.utils.toProjectId

object EventLightTable: Table("event_light") {
    val EventId = reference("event_id", EventTable, onDelete = ReferenceOption.CASCADE)
    val UserId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val CreatedAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(EventId, UserId)
}