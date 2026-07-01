package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

object SessionTable: UuidTable("session") {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE).index()
    val isTemp = bool("is_temp")
    val tokenHash = char("token_hash", 64).uniqueIndex()
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")
}