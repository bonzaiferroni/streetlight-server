package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.inList
import klutch.db.printQuery
import klutch.db.read
import klutch.db.readById
import klutch.utils.eq
import klutch.utils.toGeoPoint
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
import streetlight.model.data.GalaxyPost
import streetlight.model.data.GalaxyPostRow
import streetlight.model.data.GalaxyPostEdit
import streetlight.model.data.GalaxyPostId
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.GalaxyPostTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toGalaxyPostRow
import streetlight.server.db.tables.toLocation
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.utils.toProjectId

class GalaxyPostTableDao : DbService() {

    suspend fun readPost(galaxyPostId: GalaxyPostId) = dbQuery {
        GalaxyPostTable.read { it.id.eq(galaxyPostId) }.firstOrNull()?.toGalaxyPostRow()
    }

    suspend fun readPosts(galaxyId: GalaxyId) = dbQuery {
        GalaxyPostTable.read { GalaxyPostTable.galaxyId.eq(galaxyId) }.map { it.toGalaxyPostRow() }
    }

    suspend fun create(edit: GalaxyPostEdit) = dbQuery {
        val post = edit.toGalaxyPostRow()
        val id = GalaxyPostTable.insertAndGetId { it.writeFull(post) }.value
        GalaxyPostTable.readById(id).toGalaxyPostRow()
    }

    suspend fun update(galaxyPost: GalaxyPostRow) = dbQuery {
        GalaxyPostTable.update(where = { GalaxyPostTable.id.eq(galaxyPost.galaxyPostId) }) {
            it.writeUpdate(galaxyPost)
        } == 1
    }

    suspend fun delete(galaxyPostId: GalaxyPostId) = dbQuery {
        GalaxyPostTable.deleteWhere { GalaxyPostTable.id.eq(galaxyPostId) } == 1
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
        queryPosts(limit) { GalaxyPostTable.galaxyId.inList(galaxyIds) }
    }

    suspend fun readTopPosts(limit: Int = 100) = dbQuery {
        queryPosts(limit)
    }

    fun queryPosts(limit: Int, query: (SqlExpressionBuilder.() -> Op<Boolean>)? = null): List<GalaxyPost> {
        val now = Clock.System.now()
        val isTimelessOrUpcoming = Op.build { EventTable.startsAt.isNull() or EventTable.startsAt.greater(now) }
        val q = query?.let {
            Op.build { isTimelessOrUpcoming and query() }
        } ?: isTimelessOrUpcoming
        return GalaxyPostTable.join(LocationTable, JoinType.LEFT, GalaxyPostTable.locationId, LocationTable.id)
            .join(EventTable, JoinType.LEFT, GalaxyPostTable.eventId, EventTable.id)
            .selectAll()
            .where(q)
            .orderBy(EventTable.startsAt to SortOrder.ASC_NULLS_LAST, GalaxyPostTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toGalaxyPost() }
    }
}

fun GalaxyPostEdit.toGalaxyPostRow() = GalaxyPostRow(
    galaxyPostId = galaxyPostId ?: GalaxyPostId.random(),
    galaxyId = galaxyId ?: error("galaxyId is required"),
    username = username,
    eventId = eventId,
    locationId = locationId,
    title = title,
    text = text,
    geoPoint = geoPoint,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)

fun ResultRow.toGalaxyPost() = GalaxyPost(
    postId = this[GalaxyPostTable.id].toProjectId(),
    galaxyId = this[GalaxyPostTable.galaxyId].toProjectId(),
    username = this[GalaxyPostTable.username],
    location = this[GalaxyPostTable.locationId]?.let { toLocation() },
    event = this[GalaxyPostTable.eventId]?.let { toEvent() },
    title = this[GalaxyPostTable.title],
    text = this[GalaxyPostTable.text],
    geoPoint = this[GalaxyPostTable.geoPoint]?.toGeoPoint(),
    createdAt = this[GalaxyPostTable.createdAt],
    updatedAt = this[GalaxyPostTable.updatedAt]
)