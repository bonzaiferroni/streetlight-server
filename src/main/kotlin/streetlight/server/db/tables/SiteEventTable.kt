package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.SiteEvent
import streetlight.model.data.SiteEventId

object SiteEventTable: UuidTable("site_event") {
    val label = text("label")
    val time = timestamp("time")

    init {
        id.withDefinition("DEFAULT gen_random_uuid()")
    }
}

fun ResultRow.toSiteEvent() = SiteEvent(
    siteEventId = SiteEventId(this[SiteEventTable.id].value),
    label = this[SiteEventTable.label],
    time = this[SiteEventTable.time],
)
