package streetlight.server.model

import kampfire.api.Slug
import klutch.db.model.CallerId
import streetlight.model.data.CityContent
import streetlight.model.data.CityId
import streetlight.model.data.CityListContent
import streetlight.model.data.EntityFeed
import streetlight.model.data.Entity
import streetlight.model.data.GalaxyContent
import streetlight.model.data.HomeContent
import streetlight.server.db.services.cityRecordId
import streetlight.model.data.EntityCursor

/** The home page: the top galaxies and the page of posts at [cursor]. */
suspend fun DaoScope.readHomeContent(callerId: CallerId?, cursor: EntityCursor = EntityCursor.Default): HomeContent {
    val posts = dao.post.readHomePosts(callerId, cursor)
    val galaxies = dao.galaxy.readTopGalaxies(callerId, 3)
    return HomeContent(
        galaxies = galaxies,
        feed = readFeedMarks(posts, callerId, cursor)
    )
}

suspend fun DaoScope.readCityContent(slug: Slug, callerId: CallerId?): CityContent? {
    val city = dao.city.readCity(slug) ?: return null
    return CityContent(
        city = city,
        feed = readCityFeed(city.cityId, callerId),
    )
}

/** The page of a city's feed at [cursor], with the cursor of the next page when this one is full. */
suspend fun DaoScope.readCityFeed(
    cityId: CityId,
    callerId: CallerId?,
    cursor: EntityCursor.Time = EntityCursor.Default,
): EntityFeed {
    val entities = dao.city.readCityFeed(cityId, callerId, cursor)
    val nextCursor = entities.takeIf { it.size >= EntityCursor.DefaultLimit }?.last()?.let {
        cursor.copy(recordId = it.cityRecordId, recordAt = it.createdAt)
    }
    return EntityFeed(entities, nextCursor = nextCursor)
}

suspend fun DaoScope.readCityListContent(): CityListContent {
    val cities = dao.city.readTopCities()
    return CityListContent(cities = cities)
}

suspend fun DaoScope.readGalaxyContent(
    slug: Slug,
    callerId: CallerId?,
    cursor: EntityCursor = EntityCursor.Default
): GalaxyContent? {
    val galaxy = dao.galaxy.readGalaxy(slug, callerId) ?: return null
    val galaxyId = galaxy.galaxyId
    val posts = dao.post.readGalaxyPosts(galaxyId, callerId, cursor)
    return GalaxyContent(
        galaxy = galaxy,
        feed = readFeedMarks(posts, callerId, cursor)
    )
}

/** A feed of [posts] with the marks of their galaxies and the caller's marks on each post. */
suspend fun DaoScope.readFeedMarks(
    posts: List<Entity>,
    callerId: CallerId?,
    cursor: EntityCursor = EntityCursor.Default
): EntityFeed {
    val feedMarks = dao.galaxy.readFeedMarks(posts.mapNotNull { it.post?.galaxy?.galaxyId }.toSet())
    val postMarks = dao.post.readPostMarks(posts.mapNotNull { it.post?.postId }, callerId)
    val nextCursor = cursor.next(posts)
    return EntityFeed(posts, feedMarks, postMarks, nextCursor)
}

/** The cursor after [entities], or `null` when they did not fill a page. */
fun EntityCursor.next(entities: List<Entity>): EntityCursor? {
    val lastPost = entities.takeIf { it.size >= EntityCursor.DefaultLimit }?.last()?.post ?: return null
    return when (this) {
        is EntityCursor.Time -> copy(recordId = lastPost.postId.value, recordAt = lastPost.createdAt)
        is EntityCursor.Lean -> copy(recordId = lastPost.postId.value, postLean = lastPost.lean ?: 0)
        is EntityCursor.Mark -> copy(recordId = lastPost.postId.value, count = lastPost.markCount ?: 0)
    }
}