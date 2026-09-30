package streetlight.server.db.services

import klutch.db.DbService
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kampfire.model.Url
import kampfire.model.normalize
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import streetlight.model.data.LeadId
import streetlight.model.data.StarId
import streetlight.model.data.StarLead
import streetlight.model.data.LeadType
import streetlight.server.db.tables.LeadTable
import streetlight.server.db.tables.toLead
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** The leads the crawler is given by hand or by users. */
class LeadTableDao : DbService() {

    /** The leads due a read, oldest first: those never read, and event feeds not read within [interval]. */
    suspend fun readCheckable(interval: Duration, limit: Int) = dbQuery {
        LeadTable.selectAll().where {
            LeadTable.checkedAt.isNull() or
                (LeadTable.leadType.eq(LeadType.EventFeed) and LeadTable.checkedAt.less(Clock.System.now() - interval))
        }.orderBy(LeadTable.createdAt, SortOrder.ASC)
            .limit(limit)
            .map { it.toLead() }
    }

    /** The lead of [url], or null when there is none. */
    suspend fun readLeadByUrl(url: Url) = dbQuery {
        LeadTable.selectAll().where { LeadTable.url.eq(url.value) }.firstOrNull()?.toLead()
    }

    /** Creates the lead a star [starId] submitted, its url normalized, and returns its id. */
    suspend fun create(lead: StarLead, starId: StarId) = dbQuery {
        LeadTable.insertAndGetId {
            it[LeadTable.leadType] = lead.leadType
            it[LeadTable.url] = lead.url.normalize().value
            it[LeadTable.starId] = starId.value
            it[LeadTable.galaxyId] = lead.galaxyId?.value
            it[LeadTable.contentText] = lead.content
        }.value.let { LeadId(it) }
    }

    suspend fun updateCheckedAt(leadId: LeadId, checkedAt: Instant = Clock.System.now()) = dbQuery {
        LeadTable.update({ LeadTable.id.eq(leadId.value) }) {
            it[LeadTable.checkedAt] = checkedAt
        }
    }
}
