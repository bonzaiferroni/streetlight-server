package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.MetricResolution
import streetlight.model.data.SiteMetric
import streetlight.model.data.SiteStatus
import streetlight.model.data.SiteStatusId
import streetlight.server.utils.toRecordId

object SiteStatusTable : LongIdTable("site_status") {
    val integers = jsonb<Map<SiteMetric, Int>>("integers", tableJsonDefault)
    val doubles = jsonb<Map<SiteMetric, Double>>("doubles", tableJsonDefault)
    val resolution = enumeration<MetricResolution>("resolution")
    val coverage = float("coverage")
    val startedAt = timestamp("started_at")
    val endedAt = timestamp("ended_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toSiteStatus() = SiteStatus(
    siteStatusId = SiteStatusId(this[SiteStatusTable.id].value),
    integers = this[SiteStatusTable.integers],
    doubles = this[SiteStatusTable.doubles],
    resolution = this[SiteStatusTable.resolution],
    coverage = this[SiteStatusTable.coverage],
    endedAt = this[SiteStatusTable.endedAt],
    startedAt = this[SiteStatusTable.startedAt],
    createdAt = this[SiteStatusTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(siteStatus: SiteStatus) {
    this[SiteStatusTable.integers] = siteStatus.integers
    this[SiteStatusTable.doubles] = siteStatus.doubles
    this[SiteStatusTable.resolution] = siteStatus.resolution
    this[SiteStatusTable.coverage] = siteStatus.coverage
    this[SiteStatusTable.startedAt] = siteStatus.startedAt
    this[SiteStatusTable.endedAt] = siteStatus.endedAt
    this[SiteStatusTable.createdAt] = siteStatus.createdAt
}

// fun UpdateBuilder<*>.writeUpdate(siteStatus: SiteStatus) {
//
// }
