package streetlight.server.db.tables

import klutch.db.jsonbConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.MetricResolution
import streetlight.model.data.SiteMetric
import streetlight.model.data.StatusPoint
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

fun ResultRow.toSiteStatus() = StatusPoint(
    siteStatusId = SiteStatusId(this[SiteStatusTable.id].value),
    integers = this[SiteStatusTable.integers],
    doubles = this[SiteStatusTable.doubles],
    resolution = this[SiteStatusTable.resolution],
    coverage = this[SiteStatusTable.coverage],
    endedAt = this[SiteStatusTable.endedAt],
    startedAt = this[SiteStatusTable.startedAt],
    createdAt = this[SiteStatusTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(statusPoint: StatusPoint) {
    this[SiteStatusTable.integers] = statusPoint.integers
    this[SiteStatusTable.doubles] = statusPoint.doubles
    this[SiteStatusTable.resolution] = statusPoint.resolution
    this[SiteStatusTable.coverage] = statusPoint.coverage
    this[SiteStatusTable.startedAt] = statusPoint.startedAt
    this[SiteStatusTable.endedAt] = statusPoint.endedAt
    this[SiteStatusTable.createdAt] = statusPoint.createdAt
}

// fun UpdateBuilder<*>.writeUpdate(siteStatus: SiteStatus) {
//
// }
