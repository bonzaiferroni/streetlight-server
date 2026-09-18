package streetlight.server.db.tables

import klutch.db.jsonbConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.MetricResolution
import streetlight.model.data.SiteMetric
import streetlight.model.data.StatusStatus
import streetlight.model.data.SiteStatusId

object SiteStatusTable : LongIdTable("site_status") {
    val integers = jsonb<Map<SiteMetric, Int>>("integers", jsonbConfig)
    val doubles = jsonb<Map<SiteMetric, Double>>("doubles", jsonbConfig)
    val resolution = enumeration<MetricResolution>("resolution")
    val coverage = float("coverage")
    val startedAt = timestamp("started_at")
    val endedAt = timestamp("ended_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toSiteStatus() = StatusStatus(
    siteStatusId = SiteStatusId(this[SiteStatusTable.id].value),
    integers = this[SiteStatusTable.integers],
    doubles = this[SiteStatusTable.doubles],
    resolution = this[SiteStatusTable.resolution],
    coverage = this[SiteStatusTable.coverage],
    endedAt = this[SiteStatusTable.endedAt],
    startedAt = this[SiteStatusTable.startedAt],
    createdAt = this[SiteStatusTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(statusStatus: StatusStatus) {
    this[SiteStatusTable.integers] = statusStatus.integers
    this[SiteStatusTable.doubles] = statusStatus.doubles
    this[SiteStatusTable.resolution] = statusStatus.resolution
    this[SiteStatusTable.coverage] = statusStatus.coverage
    this[SiteStatusTable.startedAt] = statusStatus.startedAt
    this[SiteStatusTable.endedAt] = statusStatus.endedAt
    this[SiteStatusTable.createdAt] = statusStatus.createdAt
}

// fun UpdateBuilder<*>.writeUpdate(siteStatus: SiteStatus) {
//
// }
