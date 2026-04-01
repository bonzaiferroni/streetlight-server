package streetlight.server.db.services

import kampfire.model.UserId
import klutch.db.DbService
import klutch.db.inList
import klutch.db.read
import klutch.utils.UserIdentity
import klutch.utils.eq
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import streetlight.model.data.GalaxyId
import streetlight.model.data.LocationPost
import streetlight.model.data.LocationPostRow
import streetlight.model.data.LocationPostEdit
import streetlight.model.data.LocationPostId
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.LocationPostTable
import streetlight.server.db.tables.toLocation
import streetlight.server.db.tables.toLocationPostRow
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.utils.toProjectId

class LocationPostTableDao : DbService() {

    suspend fun readPostRow(locationPostId: LocationPostId) = dbQuery {
        LocationPostTable.read { LocationPostTable.id.eq(locationPostId) }.firstOrNull()?.toLocationPostRow()
    }

    suspend fun readPostRows(galaxyId: GalaxyId) = dbQuery {
        LocationPostTable.read { LocationPostTable.galaxyId.eq(galaxyId) }.map { it.toLocationPostRow() }
    }

    suspend fun create(edit: LocationPostEdit, identity: UserIdentity) = dbQuery {
        val post = edit.toLocationPostRow(identity)
        LocationPostTable.insertAndGetId { it.writeFull(post) }.toProjectId<LocationPostId>()
    }

    suspend fun update(locationPost: LocationPostRow) = dbQuery {
        LocationPostTable.update(where = { LocationPostTable.id.eq(locationPost.postId) }) {
            it.writeUpdate(locationPost)
        } == 1
    }

    suspend fun delete(locationPostId: LocationPostId) = dbQuery {
        LocationPostTable.deleteWhere { LocationPostTable.id.eq(locationPostId) } == 1
    }

    suspend fun readPosts(galaxyIds: List<GalaxyId>, limit: Int = 100) = dbQuery {
        queryPosts(limit) { LocationPostTable.galaxyId.inList(galaxyIds) }
    }

    suspend fun readPosts(galaxyId: GalaxyId, limit: Int = 100) = dbQuery {
        queryPosts(limit) { LocationPostTable.galaxyId.eq(galaxyId) }
    }

    suspend fun readPost(locationPostId: LocationPostId) = dbQuery {
        queryPosts(1) { LocationPostTable.id.eq(locationPostId) }.firstOrNull()
    }

    fun queryPosts(
        limit: Int,
        query: (SqlExpressionBuilder.() -> Op<Boolean>)? = null
    ): List<LocationPost> {
        val query = query ?: { LocationPostTable.id.isNotNull() }
        val join = LocationPostTable.join(LocationTable, JoinType.LEFT, LocationPostTable.locationId, LocationTable.id)

        return join
            .selectAll()
            .where(query)
            .orderBy(LocationPostTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toLocationPost() }
    }
}

fun LocationPostEdit.toLocationPostRow(identity: UserIdentity) = LocationPostRow(
    postId = postId ?: LocationPostId.random(),
    galaxyId = galaxyId ?: error("galaxyId is required"),
    locationId = locationId ?: error("locationId is required"),
    userId = identity.userId,
    username = identity.username,
    title = title,
    text = text,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)

fun ResultRow.toLocationPost() = LocationPost(
    postId = this[LocationPostTable.id].toProjectId(),
    galaxyId = this[LocationPostTable.galaxyId].toProjectId(),
    username = this[LocationPostTable.username],
    location = this.toLocation(),
    postTitle = this[LocationPostTable.title],
    text = this[LocationPostTable.text],
    createdAt = this[LocationPostTable.createdAt],
    updatedAt = this[LocationPostTable.updatedAt]
)
