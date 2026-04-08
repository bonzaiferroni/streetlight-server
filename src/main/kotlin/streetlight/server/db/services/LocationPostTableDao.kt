package streetlight.server.db.services

import kampfire.model.UserId
import klutch.db.DbService
import klutch.db.inList
import klutch.db.printQuery
import klutch.db.read
import klutch.utils.UserIdentity
import klutch.utils.eq
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import streetlight.model.data.GalaxyId
import streetlight.model.data.GalaxyPostResult
import streetlight.model.data.LocationPost
import streetlight.model.data.LocationPostRow
import streetlight.model.data.NewLocationPost
import streetlight.model.data.LocationPostId
import streetlight.model.data.NewGalaxyLocationPost
import streetlight.model.data.PostResult
import streetlight.server.db.tables.GalaxyLocationPostTable
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

//    suspend fun readPostRows(galaxyId: GalaxyId) = dbQuery {
//        LocationPostTable.read { LocationPostTable.galaxyId.eq(galaxyId) }.map { it.toLocationPostRow() }
//    }

    suspend fun createPost(post: NewLocationPost, identity: UserIdentity?) = dbQuery {
        val post = post.toLocationPostRow(identity)
        LocationPostTable.insertAndGetId { it.writeFull(post) }.toProjectId<LocationPostId>()
    }

    suspend fun createGalaxyPost(post: NewGalaxyLocationPost, identity: UserIdentity?) = dbQuery {
        val galaxyIds = post.galaxyIds.takeIf { it.isNotEmpty() } ?: return@dbQuery null
        val results = galaxyIds.associateWith { galaxyId ->
            val result = GalaxyLocationPostTable.insertReturning {
                it[this.galaxyId] = galaxyId.toUUID()
                it[this.postId] = post.postId.toUUID()
                it[this.userId] = identity?.userId?.toUUID()
            }

            when (result.count() > 0) {
                true -> PostResult.Posted
                else -> PostResult.Conflict // td: other results?
            }
        }
        GalaxyPostResult(results)
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
        queryPosts(limit) { GalaxyLocationPostTable.galaxyId.inList(galaxyIds) }
    }

    suspend fun readPosts(galaxyId: GalaxyId, limit: Int = 100) = dbQuery {
        queryPosts(limit) { GalaxyLocationPostTable.galaxyId.eq(galaxyId) }
    }

    suspend fun readPosts(userId: UserId, limit: Int = 100) = dbQuery {
        queryPosts(limit) { GalaxyLocationPostTable.userId.eq(userId)}
    }

    suspend fun readPost(locationPostId: LocationPostId) = dbQuery {
        queryPosts(1) { LocationPostTable.id.eq(locationPostId) }.firstOrNull()
    }

    fun queryPosts(
        limit: Int,
        query: (() -> Op<Boolean>)
    ): List<LocationPost> {
        val query = query // ?: { LocationPostTable.id.isNotNull() }
        val join = GalaxyLocationPostTable
            .join(LocationPostTable, JoinType.LEFT, GalaxyLocationPostTable.postId, LocationPostTable.id)
            .join(LocationTable, JoinType.LEFT, LocationPostTable.locationId, LocationTable.id)

        return join
            .selectAll()
            .where(query)
            .orderBy(LocationPostTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toLocationPost() }
    }
}

fun NewLocationPost.toLocationPostRow(identity: UserIdentity?) = LocationPostRow(
    postId = LocationPostId.random(),
    locationId = locationId,
    userId = identity?.userId,
    username = identity?.username,
    title = title,
    text = text,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)

fun ResultRow.toLocationPost() = LocationPost(
    postId = this[LocationPostTable.id].toProjectId(),
    galaxyId = this[GalaxyLocationPostTable.galaxyId].toProjectId(),
    username = this[LocationPostTable.username],
    location = this.toLocation(),
    postTitle = this[LocationPostTable.title],
    text = this[LocationPostTable.text],
    createdAt = this[LocationPostTable.createdAt],
    updatedAt = this[LocationPostTable.updatedAt]
)
