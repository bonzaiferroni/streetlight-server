package streetlight.server.db.services

import kampfire.model.GeoBounds
import klutch.db.DbService
import klutch.db.any
import klutch.db.count
import klutch.db.inBounds
import klutch.db.model.CallerId
import klutch.db.model.Identity
import klutch.db.printQuery
import klutch.db.readValue
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import streetlight.model.data.PostEdit
import streetlight.model.data.GalaxyId
import streetlight.model.data.PostId
import streetlight.model.data.PostType
import streetlight.model.data.StarId
import streetlight.server.db.tables.PostRecord
import streetlight.server.db.tables.PostTable
import klutch.utils.inList
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.andIfNotNull
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.FeedStatus
import streetlight.model.data.EventId
import streetlight.model.data.LocationId
import streetlight.model.data.MarkTally
import streetlight.model.data.MarkUpdate
import streetlight.model.data.MediaId
import streetlight.model.data.CuratorType
import streetlight.model.data.PostCursor
import streetlight.model.data.getCuratorType
import streetlight.server.db.tables.GalaxyHostTable
import streetlight.server.db.tables.GalaxyMarkTable
import streetlight.server.db.tables.GalaxyPostAspect
import streetlight.server.db.tables.GalaxyStarTable
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.MarkAspect
import streetlight.server.db.tables.MediaTable
import streetlight.server.db.tables.PostMarkCountTable
import streetlight.server.db.tables.PostMarkTable
import streetlight.server.db.tables.orderByCursor
import streetlight.server.db.tables.toGalaxyPost
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.toGalaxyMark
import streetlight.server.db.tables.toPost
import streetlight.server.db.tables.updateRecord
import streetlight.server.utils.toRecordId
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

    suspend fun readGalaxyPosts(galaxyIds: List<GalaxyId>, cursor: PostCursor = PostCursor.Default) = dbQuery {
        readOrderedPosts(cursor) { PostTable.galaxyId.inList(galaxyIds) }
    }

    suspend fun readGalaxyPosts(galaxyId: GalaxyId, cursor: PostCursor = PostCursor.Default) = dbQuery {
        readOrderedPosts(cursor) { PostTable.galaxyId.eq(galaxyId) }
    }

    suspend fun readStarPosts(starId: StarId, cursor: PostCursor = PostCursor.Default) = dbQuery {
        readOrderedPosts(cursor) { PostTable.starId.eq(starId.value) }
    }

    suspend fun readHomePosts(cursor: PostCursor = PostCursor.Default, callerId: CallerId) = dbQuery {
        readOrderedPosts(cursor, callerId) { GalaxyStarTable.starId.eq(callerId) }
    }

    suspend fun readPost(postId: PostId) = dbQuery {
        GalaxyPostAspect.query().where { PostTable.id.eq(postId) }.firstOrNull()?.toGalaxyPost()
    }

    suspend fun removePost(postId: PostId, identity: Identity) = dbQuery {
        PostTable.deleteWhere { PostTable.id.eq(postId) and PostTable.starId.eq(identity.callerId.value) } == 1 // td: or admin, or moderator
    }

    suspend fun readOrderedPosts(cursor: PostCursor = PostCursor.Default, callerId: CallerId? = null, filter: QueryFilter) = dbQuery {
        GalaxyPostAspect.queryCursor(cursor, callerId)
            .where { filter() andIfNotNull GalaxyPostAspect.afterCursor(cursor) }
            .orderByCursor(cursor)
            .limit(PostCursor.DefaultLimit)
            .map { it.toGalaxyPost() }
    }

    suspend fun readPostMarks(postIds: List<PostId>, callerId: CallerId?): Map<PostId, List<MarkTally>> = dbQuery {
        val isCaller = callerId?.let { PostMarkTable.starId.eq(it) } ?: Op.FALSE

        PostMarkCountTable
            .join(PostMarkTable, JoinType.LEFT, PostMarkCountTable.postId, PostMarkTable.postId,
                additionalConstraint = {
                    PostMarkTable.galaxyMarkId.eq(PostMarkCountTable.galaxyMarkId) and isCaller
                },
            )
            .select(PostMarkCountTable.postId, PostMarkCountTable.galaxyMarkId, PostMarkCountTable.count, PostMarkTable.starId)
            .where { PostMarkCountTable.postId.inList(postIds) }
            .groupBy({ PostId(it[PostMarkCountTable.postId].value) }) { row ->
                MarkTally(
                    markId = row[PostMarkCountTable.galaxyMarkId].toRecordId(),
                    count = row[PostMarkCountTable.count],
                    isMarked = row.getOrNull(PostMarkTable.starId) != null,
                )
            }
    }

    suspend fun updateMark(update: MarkUpdate, callerId: CallerId) = dbQuery {
        val isSuccess = if (update.isMarked) {
            val feedMarks = MarkAspect.queryPostMarks().where { PostTable.id.eq(update.postId) }.map { it.toGalaxyMark() }
            val curatorType = feedMarks.getCuratorType()
            val isSuccess = PostMarkTable.insertIgnore {
                it[PostMarkTable.postId] = update.postId.value
                it[PostMarkTable.galaxyMarkId] = update.markId.value
                it[PostMarkTable.starId] = callerId.value
                it[PostMarkTable.createdAt] = Clock.System.now()
            }.insertedCount > 0
            if (isSuccess && curatorType == CuratorType.Polar) {
                PostMarkTable.deleteWhere {
                    PostMarkTable.postId.eq(update.postId) and PostMarkTable.starId.eq(callerId) and
                            PostMarkTable.galaxyMarkId.neq(update.markId.value)
                }
            }
            isSuccess
        } else {
            PostMarkTable.deleteWhere {
                PostMarkTable.postId.eq(update.postId) and PostMarkTable.starId.eq(callerId) and
                        PostMarkTable.galaxyMarkId.eq(update.markId)
            } > 0
        }
        // td: calculate somewhere else
        if (isSuccess)
            updatePostMarkCount(update.postId)
        isSuccess
    }

    private fun updatePostMarkCount(postId: PostId) {
        val count = PostMarkTable.galaxyMarkId.count()
        val tallies = PostMarkTable
            .innerJoin(GalaxyMarkTable, { PostMarkTable.galaxyMarkId }, { id })
            .select(PostMarkTable.galaxyMarkId, GalaxyMarkTable.lean, count)
            .where { PostMarkTable.postId.eq(postId) }
            .groupBy(PostMarkTable.galaxyMarkId, GalaxyMarkTable.lean)
            .map { Triple(it[PostMarkTable.galaxyMarkId], it[GalaxyMarkTable.lean].value, it[count].toInt()) }

        PostMarkCountTable.deleteWhere {
            PostMarkCountTable.postId.eq(postId) and
                    PostMarkCountTable.galaxyMarkId.notInList(tallies.map { it.first })
        }
        PostMarkCountTable.batchUpsert(tallies,
            onUpdate = { it[PostMarkCountTable.count] = insertValue(PostMarkCountTable.count) }
        ) { (markId, _, markCount) ->
            this[PostMarkCountTable.postId] = postId.value
            this[PostMarkCountTable.galaxyMarkId] = markId
            this[PostMarkCountTable.count] = markCount
        }

        PostTable.update({ PostTable.id.eq(postId) }) {
            it[PostTable.lean] = tallies.sumOf { (_, lean, markCount) -> lean * markCount }
        }
    }

    suspend fun readMapPosts(bounds: GeoBounds, callerId: CallerId?) = dbQuery {
        GalaxyPostAspect.query(callerId)
            .where { LocationTable.geoPoint.inBounds(bounds) or MediaTable.geoPoint.inBounds(bounds) }
            .map { it.toGalaxyPost() }
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
