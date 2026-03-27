package streetlight.server.db.tables

import klutch.db.tables.UserTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import streetlight.model.data.EventStar
import streetlight.model.data.StarType
import streetlight.server.utils.toProjectId

object EventStarTable : Table("event_star") {
    val eventId = reference("event_id", EventTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val starType = enumeration<StarType>("star_type")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(eventId, userId)
}

fun ResultRow.toEventStar() = EventStar(
    eventId = this[EventStarTable.eventId].toProjectId(),
    value = this[EventStarTable.starType],
)