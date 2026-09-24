package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/** Addresses that cannot receive mail, and why. */
object BouncedEmailTable: LongIdTable("bounced_email") {
    val email = text("email").uniqueIndex()
    val reason = text("reason").nullable()
    val bouncedAt = timestamp("bounced_at")
}