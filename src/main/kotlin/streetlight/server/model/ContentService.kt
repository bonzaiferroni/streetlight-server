package streetlight.server.model

import kampfire.api.Slug
import kampfire.api.Username
import klutch.db.model.CallerId
import klutch.db.model.Identity
import koala.model.DocId
import org.jetbrains.exposed.v1.core.Op
import streetlight.model.data.DocContent
import streetlight.model.data.EventUpdaterContent
import streetlight.model.data.GalaxyContent
import streetlight.model.data.EntityFeed
import streetlight.model.data.FeedEntity
import streetlight.model.data.GalaxyId
import streetlight.model.data.HomeContent
import streetlight.model.data.LocationContent
import streetlight.model.data.LocationUpdaterContent
import streetlight.model.data.PostCursor
import streetlight.model.data.StarContent
import streetlight.web.doc.SiteDocTable
import streetlight.web.doc.SiteDocTree

suspend fun DaoScope.readHomeContent(callerId: CallerId?): HomeContent {
    val posts = dao.post.readOrderedPosts { Op.TRUE }
    val galaxyIds = posts.mapNotNull { it.post?.galaxy?.galaxyId }.toSet()
    val galaxies = dao.galaxy.readTopGalaxies(callerId, 3)
    val marks = dao.galaxy.readFeedMarks(galaxyIds)
    val tallies = dao.post.readPostMarks(posts.mapNotNull { it.post?.postId }, callerId)
    return HomeContent(
        galaxies = galaxies,
        feed = EntityFeed(posts, marks, tallies)
    )
}

suspend fun DaoScope.readLocationContent(slug: Slug, identity: Identity?): LocationContent? {
    val locationLayout = dao.location.readDesign(slug, identity?.callerId) ?: return null
    val location = locationLayout.location
    val events = dao.event.readLocationEvents(slug, identity?.callerId)
    val canEdit =
        location.host == null || location.host == identity?.username // || callerId?.roles?.contains(UserRole.Admin) == true
    return LocationContent(
        location = location,
        events = events,
        design = locationLayout.design,
        canEdit = canEdit,
    )
}

suspend fun DaoScope.readLocationContentBySubdomain(slug: Slug, identity: Identity?): LocationContent? {
    val locationSlug = dao.subdomain.readLocationSlug(slug) ?: return null
    return readLocationContent(locationSlug, identity)
}

suspend fun DaoScope.readLocationUpdaterContent(slug: Slug): LocationUpdaterContent? {
    val location = dao.location.readLocation(slug, null) ?: return null
    val editLogs = dao.editLog.readEdits(location.locationId)
    return LocationUpdaterContent(
        location = location,
        editLogs = editLogs
    )
}

suspend fun DaoScope.readEventUpdaterContent(slug: Slug): EventUpdaterContent? {
    val event = dao.event.readEvent(slug, null) ?: return null
    val editLogs = dao.editLog.readEdits(event.eventId)
    return EventUpdaterContent(
        event = event,
        editLogs = editLogs
    )
}

suspend fun DaoScope.readGalaxyContent(slug: Slug, callerId: CallerId?): GalaxyContent? {
    val galaxy = dao.galaxy.readGalaxy(slug, callerId) ?: return null
    val galaxyId = galaxy.galaxyId
    return GalaxyContent(
        galaxy = galaxy,
        feed = readGalaxyFeed(galaxyId, callerId)
    )
}

suspend fun DaoScope.readGalaxyFeed(
    galaxyId: GalaxyId,
    callerId: CallerId?,
    cursor: PostCursor = PostCursor.Default
): EntityFeed {
    val posts = dao.post.readOrderedPosts(galaxyId, cursor)
    val feedMarks = dao.galaxy.readFeedMarks(setOf(galaxyId))
    val postMarks = dao.post.readPostMarks(posts.mapNotNull { it.post?.postId }, callerId)
    val nextCursor = cursor.next(posts)
    return EntityFeed(posts, feedMarks, postMarks, nextCursor)
}

fun PostCursor.next(entities: List<FeedEntity>): PostCursor? = (entities.takeIf { it.size >= PostCursor.DefaultLimit }
    ?.lastOrNull()?.post)?.let { lastPost ->
        when (this) {
            is PostCursor.Time -> copy(postId = lastPost.postId, recordAt = lastPost.createdAt)
            is PostCursor.Lean -> lastPost.lean?.let { copy(postId = lastPost.postId, postLean = it) }
            is PostCursor.Mark -> lastPost.markCount?.let { copy(postId = lastPost.postId, count = it) }
        }
    }

suspend fun DaoScope.readStarContent(username: Username, caller: Identity?): StarContent? {
    val star = dao.star.readStar(username, caller?.callerId, true) ?: return null
    val posts = dao.media.readMedia(username, caller?.callerId)
    return StarContent(
        star = star,
        feed = EntityFeed(posts),
        isCaller = caller?.username == star.username
    )
}

fun DaoScope.readDocContent(docId: DocId): DocContent? {
    val node = SiteDocTree.nodes[docId] ?: return null
    return DocContent(
        node = node,
        table = SiteDocTable
    )
}