package streetlight.server.db.services

import kampfire.model.UserId
import klutch.db.DbService
import klutch.db.inList
import klutch.db.read
import klutch.utils.eq
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
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
import streetlight.server.utils.toProjectId

class EventPostTableDao : DbService() {

    suspend fun readPostRow(eventPostId: EventPostId) = dbQuery {
        EventPostTable.read { it.id.eq(eventPostId) }.firstOrNull()?.toEventPostRow()
    }

    suspend fun readPostRows(galaxyId: GalaxyId) = dbQuery {
        EventPostTable.read { EventPostTable.galaxyId.eq(galaxyId) }.map { it.toEventPostRow() }
    }

    suspend fun create(edit: EventPostEdit) = dbQuery {
        val post = edit.toEventPostRow()
        EventPostTable.insertAndGetId { it.writeFull(post) }.toProjectId<EventPostId>()
    }

    suspend fun update(galaxyPost: EventPostRow) = dbQuery {
        EventPostTable.update(where = { EventPostTable.id.eq(galaxyPost.eventPostId) }) {
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

    suspend fun readPosts(galaxyIds: List<GalaxyId>, userId: UserId?, limit: Int = 100) = dbQuery {
        queryActivePosts(userId, limit) { EventPostTable.galaxyId.inList(galaxyIds) }
    }

    suspend fun readPosts(galaxyId: GalaxyId, userId: UserId?, limit: Int = 100) = dbQuery {
        queryActivePosts(userId, limit) { EventPostTable.galaxyId.eq(galaxyId) }
    }

    suspend fun readPost(eventPostId: EventPostId, userId: UserId?) = dbQuery {
        queryPosts(userId, 1) { EventPostTable.id.eq(eventPostId) }.firstOrNull()
    }

    suspend fun readTopPosts(userId: UserId?, limit: Int = 100) = dbQuery {
        queryActivePosts(userId, limit)
    }

    fun queryActivePosts(userId: UserId?, limit: Int, query: QueryBlock? = null): List<EventPost> {
        val now = Clock.System.now()
        val isTimelessOrUpcoming = Op.build { EventTable.startsAt.isNull() or EventTable.startsAt.greater(now) }
        val q: QueryBlock = query?.let {
            { isTimelessOrUpcoming and query() }
        } ?: { isTimelessOrUpcoming }
        return queryPosts(userId, limit, q)
    }

    fun queryPosts(userId: UserId?, limit: Int, query: (SqlExpressionBuilder.() -> Op<Boolean>)? = null): List<EventPost> {
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

fun EventPostEdit.toEventPostRow() = EventPostRow(
    eventPostId = eventPostId ?: EventPostId.random(),
    galaxyId = galaxyId ?: error("galaxyId is required"),
    username = username,
    eventId = eventId ?: error("eventId is required"),
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

typealias QueryBlock = SqlExpressionBuilder.() -> Op<Boolean>