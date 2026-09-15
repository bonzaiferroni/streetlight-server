package streetlight.server.model

import kampfire.api.Slug
import klutch.db.model.CallerId
import org.jetbrains.exposed.v1.core.Op
import streetlight.model.data.EntityFeed
import streetlight.model.data.FeedEntity
import streetlight.model.data.GalaxyContent
import streetlight.model.data.HomeContent
import streetlight.model.data.PostCursor

suspend fun DaoScope.readHomeContent(callerId: CallerId?, cursor: PostCursor = PostCursor.Default): HomeContent {
    val posts = callerId?.let { dao.post.readHomePosts(cursor, callerId) } ?: dao.post.readOrderedPosts(cursor) { Op.TRUE }
    val galaxies = dao.galaxy.readTopGalaxies(callerId, 3)
    return HomeContent(
        galaxies = galaxies,
        feed = readFeedMarks(posts, callerId, cursor)
    )
}

suspend fun DaoScope.readGalaxyContent(
    slug: Slug,
    callerId: CallerId?,
    cursor: PostCursor = PostCursor.Default
): GalaxyContent? {
    val galaxy = dao.galaxy.readGalaxy(slug, callerId) ?: return null
    val galaxyId = galaxy.galaxyId
    val posts = dao.post.readGalaxyPosts(galaxyId, cursor)
    return GalaxyContent(
        galaxy = galaxy,
        feed = readFeedMarks(posts, callerId, cursor)
    )
}

suspend fun DaoScope.readFeedMarks(
    posts: List<FeedEntity>,
    callerId: CallerId?,
    cursor: PostCursor = PostCursor.Default
): EntityFeed {
    val feedMarks = dao.galaxy.readFeedMarks(posts.mapNotNull { it.post?.galaxy?.galaxyId }.toSet())
    val postMarks = dao.post.readPostMarks(posts.mapNotNull { it.post?.postId }, callerId)
    val nextCursor = cursor.next(posts)
    return EntityFeed(posts, feedMarks, postMarks, nextCursor)
}

fun PostCursor.next(entities: List<FeedEntity>): PostCursor? {
    val lastPost = entities.takeIf { it.size >= PostCursor.DefaultLimit }?.last()?.post ?: return null
    return when (this) {
        is PostCursor.Time -> copy(postId = lastPost.postId, recordAt = lastPost.createdAt)
        is PostCursor.Lean -> copy(postId = lastPost.postId, postLean = lastPost.lean ?: 0)
        is PostCursor.Mark -> copy(postId = lastPost.postId, count = lastPost.markCount ?: 0)
    }
}