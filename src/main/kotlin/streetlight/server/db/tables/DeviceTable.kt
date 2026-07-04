package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

object DeviceTable: UuidTable("device") {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE)
    val tokenHash = char("token_hash", 64).uniqueIndex()
    val lastSeenAt = timestamp("last_seen_at")
    val createdAt = timestamp("created_at")
    // label
}