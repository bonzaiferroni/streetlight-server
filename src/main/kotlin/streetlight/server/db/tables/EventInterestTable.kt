package streetlight.server.db.tables

import klutch.db.tables.UserTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import streetlight.model.data.InterestType

object EventInterestTable : Table("event_interest") {
    val eventId = reference("event_id", EventTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val interest = enumeration<InterestType>("interest")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(eventId, userId)
}