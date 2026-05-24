package streetlight.server.db.services

import kampfire.api.Slug
import klutch.db.DbService
import klutch.db.inList
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.UnionAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.unionAll
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
import streetlight.server.db.tables.PostColumns
import streetlight.server.db.tables.PostRecord
import streetlight.server.db.tables.PostTable
import streetlight.server.db.tables.SavedImageSet
import klutch.db.tables.SlugRecord
import klutch.db.tables.getSlugRecord
import klutch.db.tables.nextSlugOf
import klutch.db.tables.readColumn
import streetlight.model.data.EventId
import streetlight.model.data.LocationId
import streetlight.server.db.tables.eventJoin
import streetlight.server.db.tables.generalJoin
import streetlight.server.db.tables.toPost
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.model.StarIdentity
import streetlight.server.utils.toProjectId
import kotlin.time.Clock

class PostTableDao : DbService() {

    suspend fun createPost(edit: EventPostEdit, identity: StarIdentity) = dbQuery {
        val eventId = EventTable.readColumn(edit.eventSlug, EventTable.id).toProjectId<EventId>()

        val slug = PostTable.nextSlugOf(eventId, EventTable, EventTable.title)
        val post = edit.toPostRecord(eventId, identity, SlugRecord(slug))
        PostTable.insert { it.writeFull(post, null) }
        slug
    }

    suspend fun createPost(edit: LocationPostEdit, identity: StarIdentity?) = dbQuery {
        val locationId = LocationTable.readColumn(edit.locationSlug, LocationTable.id).toProjectId<LocationId>()

        val slug = PostTable.nextSlugOf(locationId, LocationTable, LocationTable.name)
        val post = edit.toPostRecord(locationId, identity, SlugRecord(slug))
        PostTable.insert { it.writeFull(post, null) }
        slug
    }

    suspend fun createPost(post: PostEdit, identity: StarIdentity, imageSet: SavedImageSet?) = dbQuery {
        val slug = PostTable.nextSlugOf(post.title ?: error("title not found"))
        val post = post.toPostRecord(identity, SlugRecord(slug))
        PostTable.insert { it.writeFull(post, imageSet) }
        slug
    }

    suspend fun editPost(post: PostEdit, identity: StarIdentity, imageSet: SavedImageSet?) = dbQuery {
        val title = post.title ?: error("title not found")
        val postId = post.postId ?: return@dbQuery null
        val slugSync = PostTable.getSlugRecord(postId, title)
        val post = post.toPostRecord(identity, slugSync)
        PostTable.update({ PostTable.id.eq(postId) and PostTable.starId.eq(identity.starId.value)}) {
            it.writeUpdate(post, imageSet)
        }.let { if (it == 1) slugSync.slug else null }
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
        readPosts(order, limit) { PostTable.starId.eq(userId.value) }
    }

    suspend fun readPost(postId: PostId) = dbQuery {
        queryPosts { PostTable.id.eq(postId) }.firstOrNull()?.toPost()
    }

    suspend fun readPost(slug: Slug) = dbQuery {
        queryPosts { PostTable.slug.eq(slug) }.firstOrNull()?.toPost()
    }

    suspend fun removePost(postId: PostId, identity: StarIdentity) = dbQuery {
        PostTable.deleteWhere { PostTable.id.eq(postId) and PostTable.starId.eq(identity.starId.value) } == 1 // td: or admin, or moderator
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

fun EventPostEdit.toPostRecord(eventId: EventId, identity: StarIdentity, slugRecord: SlugRecord) = PostRecord(
    postId = postId ?: PostId.random(),
    galaxyId = galaxyId,
    eventId = eventId,
    locationId = null,
    starId = identity.starId,
    slug = slugRecord.slug,
    pastSlug = slugRecord.pastSlug,
    title = null,
    subtitle = null,
    text = text,
    geoPoint = null,
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
    geoPoint = geoPoint,
    imageRef = imageRef,
    images = null,
    postType = PostType.Content,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now()
)

typealias WhereBlock = () -> Op<Boolean>

private fun orderOf(order: PostOrder) = when (order) {
    PostOrder.NewFirst -> PostTable.createdAt to SortOrder.DESC
    PostOrder.OldFirst -> PostTable.createdAt to SortOrder.ASC
    // PostOrder.Visibility -> TODO()
}