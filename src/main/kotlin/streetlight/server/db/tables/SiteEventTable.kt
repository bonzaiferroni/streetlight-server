package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

object SiteEventTable: UuidTable("site_event") {
    val label = text("label")
    val time = timestamp("time")
}