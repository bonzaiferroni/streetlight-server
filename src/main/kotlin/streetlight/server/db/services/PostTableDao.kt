package streetlight.server.db.services

import kampfire.api.Slug
import klutch.db.DbService
import klutch.db.inList
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
import streetlight.model.data.EventPostEdit
import streetlight.model.data.LocationPostEdit
import streetlight.model.data.PostId
import streetlight.model.data.PostOrder
import streetlight.model.data.PostType
import streetlight.model.data.StarId
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.PostRecord
import streetlight.server.db.tables.PostTable
import streetlight.server.db.tables.SavedImageSet
import klutch.db.tables.SlugRecord
import klutch.db.tables.getSlugRecord
import klutch.db.tables.nextSlugOf
import streetlight.model.data.EventId
import streetlight.model.data.LocationId
import streetlight.server.db.tables.PostStarTable
import streetlight.server.db.tables.postQuery
import streetlight.server.db.tables.toPost
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.updateRecord
import streetlight.server.model.StarIdentity
import kotlin.time.Clock

class PostTableDao : DbService() {

    suspend fun createPost(edit: EventPostEdit, identity: StarIdentity?) = dbQuery {
        val slug = PostTable.nextSlugOf(edit.eventId, EventTable, EventTable.title)
        val post = edit.toPostRecord(edit.eventId, identity, SlugRecord(slug))
        createPost(post, identity, null)
        slug
    }

    suspend fun createPost(edit: LocationPostEdit, identity: StarIdentity?) = dbQuery {
        val slug = PostTable.nextSlugOf(edit.locationId, LocationTable, LocationTable.name)
        val post = edit.toPostRecord(edit.locationId, identity, SlugRecord(slug))
        createPost(post, identity, null)
        slug
    }

    suspend fun createPost(post: PostEdit, identity: StarIdentity?, imageSet: SavedImageSet?) = dbQuery {
        val slug = PostTable.nextSlugOf(post.title ?: error("title not found"))
        val post = post.toPostRecord(identity, SlugRecord(slug))
        createPost(post, identity, imageSet)
        slug
    }

    private fun createPost(record: PostRecord, identity: StarIdentity?, imageSet: SavedImageSet?) {
        PostTable.insert { it.createRecord(record, imageSet) }
        identity?.starId?.let { starId ->
            PostStarTable.insert {
                it[PostStarTable.postId] = record.postId.value
                it[PostStarTable.starId] = starId.value
                it[PostStarTable.createdAt] = Clock.System.now()
            }
        }
    }

    suspend fun editPost(post: PostEdit, identity: StarIdentity, imageSet: SavedImageSet?) = dbQuery {
        val title = post.title ?: error("title not found")
        val postId = post.postId ?: return@dbQuery null
        val slugSync = PostTable.getSlugRecord(postId, title)
        val post = post.toPostRecord(identity, slugSync)
        PostTable.update({ PostTable.id.eq(postId) and PostTable.starId.eq(identity.starId.value)}) {
            it.updateRecord(post, imageSet)
        }.let { if (it == 1) slugSync.slug else null }
    }

    suspend fun delete(postId: PostId) = dbQuery {
        PostTable.deleteWhere { PostTable.id.eq(postId) } == 1
    }

    suspend fun readOrderedPosts(
        galaxyIds: List<GalaxyId>,
        callerId: StarId?,
        order: PostOrder = PostOrder.NewFirst,
        limit: Int = 100
    ) = dbQuery {
        readOrderedPosts(callerId, order, limit) { PostTable.galaxyId.inList(galaxyIds) }
    }

    suspend fun readOrderedPosts(
        galaxyId: GalaxyId,
        callerId: StarId?,
        order: PostOrder = PostOrder.NewFirst,
        limit: Int = 100
    ) = dbQuery {
        readOrderedPosts(callerId, order, limit) { PostTable.galaxyId.eq(galaxyId) }
    }

    suspend fun readStarPosts(
        starId: StarId,
        callerId: StarId?,
        order: PostOrder = PostOrder.NewFirst,
        limit: Int = 100
    ) = dbQuery {
        readOrderedPosts(callerId, order, limit) { PostTable.starId.eq(starId.value) }
    }

    suspend fun readPost(postId: PostId, callerId: StarId?) = dbQuery {
        postQuery(callerId).where { PostTable.id.eq(postId) }.firstOrNull()?.toPost()
    }

    suspend fun readPost(slug: Slug, callerId: StarId?) = dbQuery {
        postQuery(callerId).where { PostTable.slug.eq(slug) }.firstOrNull()?.toPost()
    }

    suspend fun removePost(slug: Slug, identity: StarIdentity) = dbQuery {
        PostTable.deleteWhere { PostTable.slug.eq(slug) and PostTable.starId.eq(identity.starId.value) } == 1 // td: or admin, or moderator
    }

    suspend fun readOrderedPosts(
        callerId: StarId?,
        order: PostOrder = PostOrder.NewFirst,
        limit: Int = 100,
        filter: QueryFilter? = null,
    ) = dbQuery {
        val (orderColumn, sort) = orderOf(order)

        postQuery(callerId)
            .let {
                when (filter) {
                    null -> it
                    else -> it.where(filter)
                }
            }
            .orderBy(orderColumn, sort)
            .limit(limit)
            .map { it.toPost() }
    }
}

fun EventPostEdit.toPostRecord(eventId: EventId, identity: StarIdentity?, slugRecord: SlugRecord) = PostRecord(
    postId = postId ?: PostId.random(),
    galaxyId = galaxyId,
    eventId = eventId,
    locationId = null,
    starId = identity?.starId,
    slug = slugRecord.slug,
    pastSlug = slugRecord.pastSlug,
    title = null,
    subtitle = null,
    text = text,
    geoPoint = null,
    lightCount = 0,
    imageRef = null,
    images = null,
    postType = PostType.Event,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)

fun LocationPostEdit.toPostRecord(locationId: LocationId, identity: StarIdentity?, slugRecord: SlugRecord) = PostRecord(
    postId = postId ?: PostId.random(),
    galaxyId = galaxyId,
    starId = identity?.starId,
    eventId = null,
    locationId = locationId,
    slug = slugRecord.slug,
    pastSlug = slugRecord.pastSlug,
    title = null,
    subtitle = null,
    text = text,
    geoPoint = null,
    lightCount = 0,
    imageRef = null,
    images = null,
    postType = PostType.Location,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)

fun PostEdit.toPostRecord(identity: StarIdentity?, slugRecord: SlugRecord) = PostRecord(
    postId = postId ?: PostId.random(),
    galaxyId = galaxyId,
    starId = identity?.starId,
    eventId = null,
    locationId = null,
    slug = slugRecord.slug,
    pastSlug = slugRecord.pastSlug,
    title = title ?: error("title is required"),
    subtitle = subtitle,
    text = text ?: error("text is required"),
    lightCount = 0,
    geoPoint = geoPoint,
    imageRef = imageRef,
    images = null,
    postType = PostType.Content,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now()
)

typealias QueryFilter = () -> Op<Boolean>

private fun orderOf(order: PostOrder) = when (order) {
    PostOrder.NewFirst -> PostTable.createdAt to SortOrder.DESC
    PostOrder.OldFirst -> PostTable.createdAt to SortOrder.ASC
    // PostOrder.Visibility -> TODO()
}