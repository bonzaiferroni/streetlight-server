package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.dao.id.UuidTable

object MessageTable: UuidTable("message") {
    val subject = text("subject")
    val text = text("text")
}