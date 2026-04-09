package streetlight.server.db.services

import kampfire.model.BasicUserId
import klutch.db.DbService
import klutch.db.inList
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.GalaxyId
import streetlight.model.data.EventPost
import streetlight.model.data.EventPostRow
import streetlight.model.data.EventPostEdit
import streetlight.model.data.EventPostId
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.EventPostTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toEventPostRow
import streetlight.server.db.tables.toLocation
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.model.StarIdentity
import streetlight.server.utils.toProjectId
import kotlin.time.Clock

class EventPostTableDao : DbService() {

    suspend fun readPostRow(eventPostId: EventPostId) = dbQuery {
        EventPostTable.read { it.id.eq(eventPostId) }.firstOrNull()?.toEventPostRow()
    }

    suspend fun readPostRows(galaxyId: GalaxyId) = dbQuery {
        EventPostTable.read { EventPostTable.galaxyId.eq(galaxyId) }.map { it.toEventPostRow() }
    }

    suspend fun create(edit: EventPostEdit, identity: StarIdentity) = dbQuery {
        val post = edit.toEventPostRow(identity)
        EventPostTable.insertAndGetId { it.writeFull(post) }.toProjectId<EventPostId>()
    }

    suspend fun update(galaxyPost: EventPostRow) = dbQuery {
        EventPostTable.update(where = { EventPostTable.id.eq(galaxyPost.postId) }) {
            it.writeUpdate(galaxyPost)
        } == 1
    }

    suspend fun delete(eventPostId: EventPostId) = dbQuery {
        EventPostTable.deleteWhere { EventPostTable.id.eq(eventPostId) } == 1
    }

//    suspend fun postEvent(galaxyId: GalaxyId, username: String, eventId: EventId) = dbQuery {
//        val post = GalaxyPost(
//            galaxyPostId = GalaxyPostId.random(),
//            galaxyId = galaxyId,
//            username = username,
//            eventId = eventId,
//            locationId = null,
//            text = null,
//            createdAt = Clock.System.now(),
//            updatedAt = Clock.System.now(),
//        )
//    }

    suspend fun readPosts(galaxyIds: List<GalaxyId>, limit: Int = 100) = dbQuery {
        queryActivePosts(limit) { EventPostTable.galaxyId.inList(galaxyIds) }
    }

    suspend fun readPosts(galaxyId: GalaxyId, limit: Int = 100) = dbQuery {
        queryActivePosts(limit) { EventPostTable.galaxyId.eq(galaxyId) }
    }

    suspend fun readPosts(userId: BasicUserId, limit: Int = 100) = dbQuery {
        queryPosts(limit) { EventPostTable.starId.eq(userId) }
    }

    suspend fun readPost(eventPostId: EventPostId) = dbQuery {
        queryPosts(1) { EventPostTable.id.eq(eventPostId) }.firstOrNull()
    }

    suspend fun readTopPosts(limit: Int = 100) = dbQuery {
        queryActivePosts(limit)
    }

    fun queryActivePosts(limit: Int, query: QueryBlock? = null): List<EventPost> {
        val now = Clock.System.now()
        val isTimelessOrUpcoming = EventTable.startsAt.isNull() or EventTable.startsAt.greater(now)
        val q: QueryBlock = query?.let {
            { isTimelessOrUpcoming and query() }
        } ?: { isTimelessOrUpcoming }
        return queryPosts(limit, q)
    }

    fun queryPosts(limit: Int, query: (() -> Op<Boolean>)? = null): List<EventPost> {
        val query = query ?: { EventPostTable.id.isNotNull() } // is there a better default query?
        val join = EventPostTable.join(EventTable, JoinType.LEFT, EventPostTable.eventId, EventTable.id)
            .join(LocationTable, JoinType.LEFT, EventTable.locationId, LocationTable.id)

        return join
            .selectAll()
            .where(query)
            .orderBy(EventTable.startsAt to SortOrder.ASC_NULLS_LAST, EventPostTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toEventPost() }
    }
}

fun EventPostEdit.toEventPostRow(
    identity: StarIdentity,
) = EventPostRow(
    postId = postId ?: EventPostId.random(),
    galaxyId = galaxyId ?: error("galaxyId is required"),
    eventId = eventId ?: error("eventId is required"),
    starId = identity.userId,
    username = identity.username,
    text = text,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)

fun ResultRow.toEventPost() = EventPost(
    postId = this[EventPostTable.id].toProjectId(),
    galaxyId = this[EventPostTable.galaxyId].toProjectId(),
    username = this[EventPostTable.username],
    event = this.toEvent(),
    location = this.toLocation(),
    text = this[EventPostTable.text],
    createdAt = this[EventPostTable.createdAt],
    updatedAt = this[EventPostTable.updatedAt]
)

typealias QueryBlock = () -> Op<Boolean>