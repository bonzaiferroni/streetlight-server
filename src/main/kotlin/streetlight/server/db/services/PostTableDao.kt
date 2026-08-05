package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.any
import klutch.db.count
import klutch.db.model.CallerId
import klutch.db.model.Identity
import klutch.db.readValue
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
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
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.FeedStatus
import streetlight.model.data.EventId
import streetlight.model.data.LocationId
import streetlight.model.data.MediaId
import streetlight.server.db.tables.GalaxyHostTable
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.PostStarTable
import streetlight.server.db.tables.galaxyPostQuery
import streetlight.server.db.tables.toGalaxyPost
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.toPost
import streetlight.server.db.tables.updateRecord
import streetlight.server.utils.toStarId
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

    suspend fun create(post: PostEdit, callerId: CallerId?) = dbQuery {
        val record = post.toPostRecord(callerId)
        val status = callerId?.let {
            getInitialContentStatus(record.galaxyId, callerId)
        } ?: FeedStatus.Live // td: figure out feedstatus for no callerId

        PostTable.insert { it.createRecord(record, status) }
        callerId?.let {
            PostStarTable.insert {
                it[PostStarTable.postId] = record.postId.value
                it[PostStarTable.starId] = callerId.value
                it[PostStarTable.createdAt] = Clock.System.now()
            }
        }
        PostTable.selectAll().where { PostTable.id.eq(record.postId) }.singleOrNull()?.toPost()
    }

    suspend fun update(postId: PostId, post: PostEdit, callerId: CallerId) = dbQuery {
        val post = post.toPostRecord(callerId)
        PostTable.updateReturning(where = { PostTable.id.eq(postId) and PostTable.starId.eq(callerId) }) {
            it.updateRecord(post)
        }.singleOrNull()?.toPost()
    }

    private fun getInitialContentStatus(galaxyId: GalaxyId, callerId: CallerId): FeedStatus {
        if (GalaxyHostTable.any { it.galaxyId.eq(galaxyId) and it.hostId.eq(callerId) })
            return FeedStatus.Live

        val reviewCount = GalaxyTable.readValue(GalaxyTable.reviewCount) { it.id.eq(galaxyId) }
        val postCount = PostTable.count {
                it.galaxyId.eq(galaxyId) and it.starId.eq(callerId) and it.status.eq(FeedStatus.Live)
            }

        return if (postCount >= reviewCount) FeedStatus.Live else FeedStatus.Reviewing
    }

    suspend fun delete(postId: PostId) = dbQuery {
        PostTable.deleteWhere { PostTable.id.eq(postId) } == 1
    }

    suspend fun readOrderedPosts(
        galaxyIds: List<GalaxyId>,
        callerId: CallerId?,
        order: PostOrder = PostOrder.NewFirst,
        filterUpcomingEvents: Boolean = true,
        limit: Int = 100
    ) = dbQuery {
        readOrderedPosts(callerId, order, filterUpcomingEvents, limit) { PostTable.galaxyId.inList(galaxyIds) }
    }

    suspend fun readOrderedPosts(
        galaxyId: GalaxyId,
        callerId: CallerId?,
        order: PostOrder = PostOrder.NewFirst,
        filterUpcomingEvents: Boolean = true,
        limit: Int = 100
    ) = dbQuery {
        readOrderedPosts(callerId, order, filterUpcomingEvents, limit) { PostTable.galaxyId.eq(galaxyId) }
    }

    suspend fun readStarPosts(
        starId: StarId,
        callerId: CallerId?,
        order: PostOrder = PostOrder.NewFirst,
        filterUpcomingEvents: Boolean = true,
        limit: Int = 100
    ) = dbQuery {
        readOrderedPosts(callerId, order, filterUpcomingEvents, limit) { PostTable.starId.eq(starId.value) }
    }

    suspend fun readPost(postId: PostId, callerId: CallerId?) = dbQuery {
        galaxyPostQuery(callerId).where { PostTable.id.eq(postId) }.firstOrNull()?.toGalaxyPost()
    }

    suspend fun removePost(postId: PostId, identity: Identity) = dbQuery {
        PostTable.deleteWhere { PostTable.id.eq(postId) and PostTable.starId.eq(identity.callerId.value) } == 1 // td: or admin, or moderator
    }

    suspend fun readOrderedPosts(
        callerId: CallerId?,
        order: PostOrder = PostOrder.NewFirst,
        filterUpcomingEvents: Boolean = true,
        limit: Int = 100,
        filter: QueryFilter? = null,
    ) = dbQuery {
        val (orderColumn, sort) = orderOf(order)

        val filter = getFilter(filterUpcomingEvents, filter)

        galaxyPostQuery(callerId)
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

fun PostEdit.toPostRecord(callerId: CallerId?) = PostRecord(
    postId = postId ?: PostId.random(),
    galaxyId = galaxyId,
    eventId = recordId.takeIf { postType == PostType.Event }?.let { EventId(recordId) },
    locationId = recordId.takeIf { postType == PostType.Location }?.let { LocationId(recordId) },
    mediaId = recordId.takeIf { postType == PostType.Media }?.let { MediaId(recordId) },
    starId = callerId?.toStarId(),
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