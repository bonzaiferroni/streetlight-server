package streetlight.server.db.tables

import klutch.db.tables.UserTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object StarTable: UUIDTable("star") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)

}