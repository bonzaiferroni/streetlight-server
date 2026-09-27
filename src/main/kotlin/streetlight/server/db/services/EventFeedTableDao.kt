package streetlight.server.db.services

import klutch.db.DbService
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.EventFeedId
import streetlight.server.db.tables.EventFeedTable
import streetlight.server.db.tables.toGeneralEventFeed
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** The pages listing local events at many locations. */
class EventFeedTableDao : DbService() {

    /** The feeds not checked within [interval], oldest first. */
    suspend fun readCheckable(interval: Duration) = dbQuery {
        EventFeedTable.selectAll().where {
            EventFeedTable.checkedAt.isNull() or EventFeedTable.checkedAt.less(Clock.System.now() - interval)
        }.orderBy(EventFeedTable.createdAt, SortOrder.ASC).map { it.toGeneralEventFeed() }
    }

    suspend fun updateCheckedAt(eventFeedId: EventFeedId, checkedAt: Instant = Clock.System.now()) = dbQuery {
        EventFeedTable.update({ EventFeedTable.id.eq(eventFeedId.value) }) {
            it[EventFeedTable.checkedAt] = checkedAt
        }
    }
}
