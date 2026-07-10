package streetlight.server.db.services

import io.github.oshai.kotlinlogging.KotlinLogging
import klutch.db.DbService
import klutch.db.deleteSingle
import klutch.db.mapFirstOrNull
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.MetricResolution
import streetlight.model.data.SiteStatus
import streetlight.model.data.SiteStatusId
import streetlight.server.db.tables.SiteStatusTable
import streetlight.server.db.tables.toSiteStatus
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import kotlin.time.Instant

private val log = KotlinLogging.logger(SiteStatusTableDao::class.simpleName!!)

class SiteStatusTableDao : DbService() {

    suspend fun create(siteStatus: SiteStatus) = dbQuery {
        SiteStatusTable.insert {
            it.writeFull(siteStatus)
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
    ): List<SiteStatus> = dbQuery {
        SiteStatusTable.selectAll().where {
            SiteStatusTable.resolution.eq(resolution) and
                    SiteStatusTable.startedAt.greaterEq(startedAt) and
                    SiteStatusTable.endedAt.lessEq(endedAt)
        }.map { it.toSiteStatus() }
    }
}
