package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.inList
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.UnionAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.unionAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.GalaxyId
import streetlight.model.data.EventPostEdit
import streetlight.model.data.LocationPostEdit
import streetlight.model.data.PostId
import streetlight.model.data.PostOrder
import streetlight.model.data.PostType
import streetlight.model.data.StarId
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.PostColumns
import streetlight.server.db.tables.PostRow
import streetlight.server.db.tables.PostTable
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toPostRow
import streetlight.server.db.tables.toPost
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.model.StarIdentity
import streetlight.server.utils.toProjectId
import java.sql.SQLIntegrityConstraintViolationException
import kotlin.time.Clock

class PostTableDao : DbService() {

    suspend fun readPostRow(postId: PostId) = dbQuery {
        PostTable.read { it.id.eq(postId) }.firstOrNull()?.toPostRow()
    }

    suspend fun readPostRows(galaxyId: GalaxyId) = dbQuery {
        PostTable.read { PostTable.galaxyId.eq(galaxyId) }.map { it.toPostRow() }
    }

    suspend fun create(edit: EventPostEdit, identity: StarIdentity): PostId? = dbQuery {
        val post = edit.toPostRow(identity)
        try {
            PostTable.insertAndGetId { it.writeFull(post) }.toProjectId<PostId>()
        } catch (e: ExposedSQLException) {
            if (e.cause is SQLIntegrityConstraintViolationException) null else throw e
        }
    }

    suspend fun createPost(post: LocationPostEdit, identity: StarIdentity?) = dbQuery {
        val post = post.toPostRow(identity)
        PostTable.insertAndGetId { it.writeFull(post) }.toProjectId<PostId>()
    }

    suspend fun update(galaxyPost: PostRow) = dbQuery {
        PostTable.update(where = { PostTable.id.eq(galaxyPost.postId) }) {
            it.writeUpdate(galaxyPost)
        } == 1
    }

    suspend fun delete(postId: PostId) = dbQuery {
        PostTable.deleteWhere { PostTable.id.eq(postId) } == 1
    }

    suspend fun readActivePosts(
        galaxyIds: List<GalaxyId>,
        order: PostOrder = PostOrder.NewFirst,
        limit: Int = 100
    ) = dbQuery {
        readActivePosts(order, limit) { PostTable.galaxyId.inList(galaxyIds) }
    }

    suspend fun readActivePosts(
        galaxyId: GalaxyId,
        order: PostOrder = PostOrder.NewFirst,
        limit: Int = 100
    ) = dbQuery {
        readActivePosts(order, limit) { PostTable.galaxyId.eq(galaxyId) }
    }

    suspend fun readStarPosts(
        userId: StarId,
        order: PostOrder = PostOrder.NewFirst,
        limit: Int = 100
    ) = dbQuery {
        readPosts(order, limit) { PostTable.starId.eq(userId) }
    }

    suspend fun readPost(postId: PostId) = dbQuery {
        queryPosts { PostTable.id.eq(postId) }.firstOrNull()?.toPost()
    }

    suspend fun readActivePosts(
        order: PostOrder = PostOrder.NewFirst,
        limit: Int = 100,
        where: WhereBlock? = null,
    ) = dbQuery {
        val (orderColumn, sort) = orderOf(order)

        val eventQuery = eventJoin()
            .select(PostColumns)
            .where {
                val base = PostTable.postType.eq(PostType.Event) and EventTable.startsAt.greaterEq(Clock.System.now())
                where?.let { base and it() } ?: base
            }

        val generalQuery = generalJoin()
            .select(PostColumns)
            .where {
                val base = PostTable.postType.neq(PostType.Event)
                where?.let { base and it() } ?: base
            }
            .orderBy(orderColumn, sort)
            .limit(limit)

        eventQuery.unionAll(generalQuery)
            .map { it.toPost() }.sortedByDescending { it.createdAt }
    }

    suspend fun readPosts(
        order: PostOrder = PostOrder.NewFirst,
        limit: Int = 100,
        where: WhereBlock? = null,
    ) = dbQuery {
        val (orderColumn, sort) = orderOf(order)

        queryPosts(where)
            .orderBy(orderColumn, sort)
            .limit(limit)
            .map { it.toPost() }
    }

    fun queryPosts(
        where: WhereBlock? = null,
    ): UnionAll {
        val events = eventJoin().select(PostColumns)
            .where {
                val base = PostTable.postType.eq(PostType.Event)
                where?.let { base and it() } ?: base
            }

        val general = generalJoin().select(PostColumns)
            .where {
                val base = PostTable.postType.neq(PostType.Event)
                where?.let { base and it() } ?: base
            }

        return events.unionAll(general)
    }
}

fun EventPostEdit.toPostRow(
    identity: StarIdentity,
) = PostRow(
    postId = postId ?: PostId.random(),
    galaxyId = galaxyId ?: error("galaxyId is required"),
    eventId = eventId ?: error("eventId is required"),
    locationId = null,
    starId = identity.userId,
    title = null,
    text = text,
    postType = PostType.Event,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)

fun LocationPostEdit.toPostRow(identity: StarIdentity?) = PostRow(
    postId = PostId.random(),
    galaxyId = galaxyId ?: error("galaxyId is required"),
    starId = identity?.userId,
    eventId = null,
    locationId = locationId,
    title = null,
    text = text,
    postType = PostType.Location,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)

typealias WhereBlock = () -> Op<Boolean>

private fun eventJoin() = PostTable
    .join(StarTable, JoinType.LEFT, PostTable.starId, StarTable.id)
    .join(EventTable, JoinType.LEFT, PostTable.eventId, EventTable.id)
    .join(LocationTable, JoinType.LEFT, EventTable.locationId, LocationTable.id)

private fun generalJoin() = PostTable
    .join(StarTable, JoinType.LEFT, PostTable.starId, StarTable.id)
    .join(EventTable, JoinType.LEFT, PostTable.eventId, EventTable.id)
    .join(LocationTable, JoinType.LEFT, PostTable.locationId, LocationTable.id)

private fun orderOf(order: PostOrder) = when (order) {
    PostOrder.NewFirst -> PostTable.createdAt to SortOrder.DESC
    PostOrder.OldFirst -> PostTable.createdAt to SortOrder.ASC
    // PostOrder.Visibility -> TODO()
}