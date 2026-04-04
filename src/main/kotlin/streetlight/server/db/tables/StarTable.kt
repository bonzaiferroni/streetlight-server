package streetlight.server.db.tables

import klutch.db.tables.UserTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import streetlight.model.data.Star

// this provides additional properties for User, likely will become the only table for account information
object StarTable: UUIDTable("star") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val description = text("description").nullable()
    val thumbUrl = text("thumb_url").nullable()
}

fun ResultRow.toStar() = Star(
    name = this[UserTable.username],
    description = this[StarTable.description],
    imageUrl = this[UserTable.avatarUrl],
    thumbUrl = this[StarTable.thumbUrl],
    updatedAt = this[UserTable.updatedAt],
    createdAt = this[UserTable.createdAt],
)