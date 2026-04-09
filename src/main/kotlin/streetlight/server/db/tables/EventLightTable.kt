package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object EventLightTable: Table("event_light") {
    val EventId = reference("event_id", EventTable, onDelete = ReferenceOption.CASCADE)
    val StarId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE)
    val CreatedAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(EventId, StarId)
}