package streetlight.server.db.services

import io.github.oshai.kotlinlogging.KotlinLogging
import klutch.db.DbService
import klutch.db.deleteSingle
import klutch.db.mapFirstOrNull
import klutch.db.whereWith
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.model.data.MetricResolution
import streetlight.model.data.StatusPoint
import streetlight.model.data.SiteStatusId
import streetlight.server.db.tables.SiteStatusTable
import streetlight.server.db.tables.toSiteStatus
import streetlight.server.db.tables.writeFull
import streetlight.server.plugins.logger
import kotlin.time.Instant

class SiteStatusTableDao : DbService() {

    suspend fun create(statusPoint: StatusPoint) = dbQuery {
        SiteStatusTable.insert {
            it.writeFull(statusPoint)
        }
    }

    suspend fun readSiteStatus(siteStatusId: SiteStatusId) = dbQuery {
        SiteStatusTable.selectAll().where { SiteStatusTable.id.eq(siteStatusId) }
            .mapFirstOrNull { it.toSiteStatus() }
    }

    suspend fun delete(siteStatusId: SiteStatusId): Boolean = dbQuery {
        SiteStatusTable.deleteSingle { SiteStatusTable.id.eq(siteStatusId) }
    }

    suspend fun readByResolutionAndPeriod(
        resolution: MetricResolution,
        startedAt: Instant,
        endedAt: Instant
    ) = dbQuery {
        SiteStatusTable.selectAll().where {
            SiteStatusTable.resolution.eq(resolution) and
                    SiteStatusTable.startedAt.greaterEq(startedAt) and
                    SiteStatusTable.endedAt.lessEq(endedAt)
        }.map { it.toSiteStatus() }
    }

    suspend fun readByResolution(
        resolution: MetricResolution,
        limit: Int = 60
    ) = dbQuery {
        SiteStatusTable.selectAll().where {
            SiteStatusTable.resolution.eq(resolution)
        }.orderBy(SiteStatusTable.id, SortOrder.DESC).limit(limit)
            .map { it.toSiteStatus() }
    }

    suspend fun readEvents(resolution: MetricResolution) {

    }
}

private val log = KotlinLogging.logger(SiteStatusTableDao::class)