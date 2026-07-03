package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.any
import klutch.db.count
import klutch.db.readValue
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.PostEdit
import streetlight.model.data.GalaxyId
import streetlight.model.data.PostId
import streetlight.model.data.PostOrder
import streetlight.model.data.PostType
import streetlight.model.data.StarId
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.PostRecord
import streetlight.server.db.tables.PostTable
import klutch.utils.inList
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import streetlight.model.data.FeedStatus
import streetlight.model.data.EventId
import streetlight.model.data.LocationId
import streetlight.model.data.MediaId
import streetlight.server.db.tables.GalaxyHostTable
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.PostStarTable
import streetlight.server.db.tables.postQuery
import streetlight.server.db.tables.toGalaxyPost
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.updateRecord
import streetlight.server.model.StarIdentity
import kotlin.time.Clock

class PostTableDao : DbService() {

    // suspend fun createPost(edit: EventPostEdit, identity: StarIdentity) = dbQuery {
    //     val slug = PostTable.nextSlugOf(edit.eventId, EventTable, EventTable.title)
    //     val post = edit.toPostRecord(edit.eventId, identity, SlugRecord(slug))
    //     createPost(post, identity)
    //     slug
    // }

    // suspend fun createPost(edit: LocationPostEdit, identity: StarIdentity) = dbQuery {
    //     val slug = PostTable.nextSlugOf(edit.locationId, LocationTable, LocationTable.name)
    //     val post = edit.toPostRecord(edit.locationId, identity, SlugRecord(slug))
    //     createPost(post, identity, null)
    //     slug
    // }

    suspend fun createPost(post: PostEdit, caller: StarIdentity) = dbQuery {
        val record = post.toPostRecord(caller)
        val status = getInitialContentStatus(record.galaxyId, caller.starId)

        PostTable.insert { it.createRecord(record, status) }
        PostStarTable.insert {
            it[PostStarTable.postId] = record.postId.value
            it[PostStarTable.starId] = caller.starId.value
            it[PostStarTable.createdAt] = Clock.System.now()
        }
    }

    private fun getInitialContentStatus(galaxyId: GalaxyId, callerId: StarId): FeedStatus {
        if (GalaxyHostTable.any { it.galaxyId.eq(galaxyId) and it.hostId.eq(callerId) })
            return FeedStatus.Live

        val reviewCount = GalaxyTable.readValue(GalaxyTable.reviewCount) { it.id.eq(galaxyId) }
        val postCount = PostTable.count {
                it.galaxyId.eq(galaxyId) and it.starId.eq(callerId) and it.status.eq(FeedStatus.Live)
            }

        return if (postCount >= reviewCount) FeedStatus.Live else FeedStatus.Reviewing
    }

    suspend fun editPost(postId: PostId, post: PostEdit, identity: StarIdentity) = dbQuery {
        val post = post.toPostRecord(identity)
        PostTable.update({ PostTable.id.eq(postId) and PostTable.starId.eq(identity.starId.value) }) {
            it.updateRecord(post)
        }
        postId
    }

    suspend fun delete(postId: PostId) = dbQuery {
        PostTable.deleteWhere { PostTable.id.eq(postId) } == 1
    }

    suspend fun readOrderedPosts(
        galaxyIds: List<GalaxyId>,
        callerId: StarId?,
        order: PostOrder = PostOrder.NewFirst,
        filterUpcomingEvents: Boolean = true,
        limit: Int = 100
    ) = dbQuery {
        readOrderedPosts(callerId, order, filterUpcomingEvents, limit) { PostTable.galaxyId.inList(galaxyIds) }
    }

    suspend fun readOrderedPosts(
        galaxyId: GalaxyId,
        callerId: StarId?,
        order: PostOrder = PostOrder.NewFirst,
        filterUpcomingEvents: Boolean = true,
        limit: Int = 100
    ) = dbQuery {
        readOrderedPosts(callerId, order, filterUpcomingEvents, limit) { PostTable.galaxyId.eq(galaxyId) }
    }

    suspend fun readStarPosts(
        starId: StarId,
        callerId: StarId?,
        order: PostOrder = PostOrder.NewFirst,
        filterUpcomingEvents: Boolean = true,
        limit: Int = 100
    ) = dbQuery {
        readOrderedPosts(callerId, order, filterUpcomingEvents, limit) { PostTable.starId.eq(starId.value) }
    }

    suspend fun readPost(postId: PostId, callerId: StarId?) = dbQuery {
        postQuery(callerId).where { PostTable.id.eq(postId) }.firstOrNull()?.toGalaxyPost()
    }

    suspend fun removePost(postId: PostId, identity: StarIdentity) = dbQuery {
        PostTable.deleteWhere { PostTable.id.eq(postId) and PostTable.starId.eq(identity.starId.value) } == 1 // td: or admin, or moderator
    }

    suspend fun readOrderedPosts(
        callerId: StarId?,
        order: PostOrder = PostOrder.NewFirst,
        filterUpcomingEvents: Boolean = true,
        limit: Int = 100,
        filter: QueryFilter? = null,
    ) = dbQuery {
        val (orderColumn, sort) = orderOf(order)

        val filter = getFilter(filterUpcomingEvents, filter)

        postQuery(callerId)
            .let {
                when (filter) {
                    null -> it
                    else -> it.where(filter)
                }
            }
            .orderBy(orderColumn, sort)
            .limit(limit)
            .map { it.toGalaxyPost() }
    }

    private fun getFilter(filterUpcomingEvents: Boolean, filter: QueryFilter?): QueryFilter? {
        val eventFilter: QueryFilter? = if (filterUpcomingEvents) {
            { PostTable.postType.neq(PostType.Event) or EventTable.startsAt.greater(Clock.System.now()) }
        } else null

        if (eventFilter == null) return filter
        if (filter == null) return eventFilter
        return { eventFilter() and filter() }
    }
}

fun PostEdit.toPostRecord(identity: StarIdentity?) = PostRecord(
    postId = postId ?: PostId.random(),
    galaxyId = galaxyId,
    eventId = recordId.takeIf { postType == PostType.Event }?.let { EventId(recordId) },
    locationId = recordId.takeIf { postType == PostType.Location }?.let { LocationId(recordId) },
    mediaId = recordId.takeIf { postType == PostType.Media }?.let { MediaId(recordId) },
    starId = identity?.starId,
    title = title,
    text = text,
    lightCount = 0,
    postType = postType,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)

typealias QueryFilter = () -> Op<Boolean>

private fun orderOf(order: PostOrder) = when (order) {
    PostOrder.NewFirst -> PostTable.createdAt to SortOrder.DESC
    PostOrder.OldFirst -> PostTable.createdAt to SortOrder.ASC
    // PostOrder.Visibility -> TODO()
}