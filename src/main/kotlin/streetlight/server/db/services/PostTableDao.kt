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
import streetlight.server.db.tables.PostRecord
import streetlight.server.db.tables.PostTable
import klutch.utils.inList
import org.jetbrains.exposed.v1.core.Case
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.Sum
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.FeedStatus
import streetlight.model.data.EventId
import streetlight.model.data.LocationId
import streetlight.model.data.PostMark
import streetlight.model.data.MediaId
import streetlight.server.db.tables.GalaxyHostTable
import streetlight.server.db.tables.GalaxyMarkTable
import streetlight.server.db.tables.GalaxyPostAspect
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.MarkAspect
import streetlight.server.db.tables.PostMarkTable
import streetlight.server.db.tables.toGalaxyPost
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.toGalaxyMark
import streetlight.server.db.tables.toPost
import streetlight.server.db.tables.updateRecord
import streetlight.server.utils.toRecordId
import streetlight.server.utils.toStarId
import kotlin.time.Clock
import kotlin.uuid.Uuid

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
            PostMarkTable.insert {
                it[PostMarkTable.postId] = record.postId.value
                it[PostMarkTable.starId] = callerId.value
                it[PostMarkTable.createdAt] = Clock.System.now()
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
        limit: Int = 100
    ) = dbQuery {
        readOrderedPosts(order, limit) { PostTable.galaxyId.inList(galaxyIds) }
    }

    suspend fun readOrderedPosts(
        galaxyId: GalaxyId,
        callerId: CallerId?,
        order: PostOrder = PostOrder.NewFirst,
        limit: Int = 100
    ) = dbQuery {
        readOrderedPosts(order, limit) { PostTable.galaxyId.eq(galaxyId) }
    }

    suspend fun readStarPosts(
        starId: StarId,
        callerId: CallerId?,
        order: PostOrder = PostOrder.NewFirst,
        limit: Int = 100
    ) = dbQuery {
        readOrderedPosts(order, limit) { PostTable.starId.eq(starId.value) }
    }

    suspend fun readPost(postId: PostId) = dbQuery {
        GalaxyPostAspect.query().where { PostTable.id.eq(postId) }.firstOrNull()?.toGalaxyPost()
    }

    suspend fun removePost(postId: PostId, identity: Identity) = dbQuery {
        PostTable.deleteWhere { PostTable.id.eq(postId) and PostTable.starId.eq(identity.callerId.value) } == 1 // td: or admin, or moderator
    }

    suspend fun readOrderedPosts(
        order: PostOrder = PostOrder.NewFirst,
        limit: Int = 100,
        filter: QueryFilter,
    ) = dbQuery {
        val (orderColumn, sort) = orderOf(order)

        GalaxyPostAspect.query()
            .where(filter)
            .orderBy(orderColumn, sort)
            .limit(limit)
            .map { it.toGalaxyPost() }
    }

    suspend fun readPostMarks(postIds: List<PostId>, callerId: CallerId?): Map<PostId, List<PostMark>> = dbQuery {
        val sum = PostMarkTable.markId.count()
        val isCaller = callerId?.let { PostMarkTable.starId.eq(it) } ?: Op.FALSE

        val callerMarks = Sum(
            Case()
                .When(isCaller, intLiteral(1))
                .Else(intLiteral(0)),
            IntegerColumnType(),
        )

        PostMarkTable
            .select(PostMarkTable.postId, PostMarkTable.markId, sum, callerMarks)
            .where { PostMarkTable.postId.inList(postIds) }
            .groupBy(PostMarkTable.postId, PostMarkTable.markId)
            .groupBy({ PostId(it[PostMarkTable.postId].value) }) { row ->
                PostMark(
                    markId = row[PostMarkTable.markId].toRecordId(),
                    sum = row[sum].toInt(),
                    isMarked = (row[callerMarks] ?: 0) > 0,
                )
            }
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