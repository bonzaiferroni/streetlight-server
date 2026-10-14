package streetlight.server.service

import streetlight.server.model.DaoScope
import streetlight.server.model.dao
import kampfire.api.Slug
import klutch.db.model.CallerId
import streetlight.model.data.CityContent
import streetlight.model.data.CityId
import streetlight.model.data.CityFeedContent
import streetlight.model.data.GalaxyFeedContent
import streetlight.model.data.EntityFeed
import streetlight.model.data.FeedContext
import streetlight.model.data.FeedType
import streetlight.model.data.FeedRequest
import streetlight.model.data.FeedSource
import streetlight.model.data.GalaxyId
import streetlight.model.data.Entity
import streetlight.model.data.GalaxyContent
import streetlight.model.data.HomeContent
import streetlight.model.data.EntityCursor
import streetlight.model.data.EventLocation
import streetlight.model.data.Location
import streetlight.model.data.LocationId
import streetlight.model.data.MapQuery
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * The home page: the top galaxies, and the first page of the [feed] type, or by default of the posts of the galaxies
 * a caller follows, or of upcoming events without one. A visitor is not offered the posts of followed galaxies.
 */
suspend fun DaoScope.readHomeContent(callerId: CallerId?, feed: FeedType? = null): HomeContent {
    val galaxies = dao.galaxy.readTopGalaxies(callerId, 3)
    val types = callerId?.let { FeedContext.Home.types } ?: (FeedContext.Home.types - FeedType.GalaxyPosts)
    val type = feed?.takeIf { it in types } ?: callerId?.let { FeedType.GalaxyPosts } ?: FeedType.Events

    val homeFeed = when (type) {
        FeedType.GalaxyPosts -> readHomePostFeed(callerId)
        FeedType.Media -> readMediaFeed(FeedContext.Home)
        else -> readEventFeed(callerId = callerId)
    }
    return HomeContent(galaxies = galaxies, feed = homeFeed.copy(types = types))
}

/** The page of the feed [request] names, as seen by [callerId], or `null` for a type its context does not offer. */
suspend fun DaoScope.readFeed(request: FeedRequest, callerId: CallerId?): EntityFeed? {
    val (source, cursor) = request
    if (source.type !in source.context.types) return null

    return when (val context = source.context) {
        FeedContext.Home -> when (source.type) {
            FeedType.Events -> readEventFeed(cursor, callerId)
            FeedType.Media -> readMediaFeed(context, cursor)
            FeedType.GalaxyPosts -> readHomePostFeed(callerId, cursor)
            else -> null
        }
        is FeedContext.City -> when (source.type) {
            FeedType.Events -> readCityFeed(context.cityId, callerId, cursor)
            FeedType.Locations -> readCityLocationFeed(context.cityId, callerId, cursor)
            else -> null
        }
        is FeedContext.Location -> readLocationEventFeed(context.locationId, callerId, cursor)
        is FeedContext.Galaxy -> readGalaxyFeed(context.galaxyId, callerId, cursor)
        is FeedContext.Star -> readMediaFeed(context, cursor)
        FeedContext.Galaxies -> readGalaxiesFeed(source.type, callerId, cursor)
        FeedContext.Cities -> readCitiesFeed(cursor)
    }
}

/**
 * The galaxy feed with the first page of its [feed] type, or by default of the galaxies a caller has lit, or of the
 * top galaxies without one. A visitor is not offered the galaxies of a star.
 */
suspend fun DaoScope.readGalaxyFeedContent(callerId: CallerId?, feed: FeedType? = null): GalaxyFeedContent {
    val types = callerId?.let { FeedContext.Galaxies.types } ?: (FeedContext.Galaxies.types - FeedType.StarGalaxies)
    val type = feed?.takeIf { it in types } ?: callerId?.let { FeedType.StarGalaxies } ?: FeedType.TopGalaxies
    val galaxyFeed = readGalaxiesFeed(type, callerId) ?: EntityFeed(emptyList(), source = FeedSource(FeedContext.Galaxies, type))
    return GalaxyFeedContent(galaxyFeed.copy(types = types))
}

/**
 * The page at [cursor] of the galaxies of [type]: those [callerId] has lit, by name; the top, by star count; or the
 * newest. A cursor of another kind starts the first page, keeping its search. `null` for a type of another context.
 */
suspend fun DaoScope.readGalaxiesFeed(
    type: FeedType,
    callerId: CallerId?,
    cursor: EntityCursor? = null,
): EntityFeed? {
    val source = FeedSource(FeedContext.Galaxies, type)
    val search = cursor?.search
    return when (type) {
        FeedType.StarGalaxies -> {
            val nameCursor = cursor as? EntityCursor.Name ?: EntityCursor.Name.Default.copy(search = search)
            val galaxies = callerId?.let { dao.galaxy.readStarGalaxies(it, nameCursor) } ?: emptyList()
            pagedFeedOf(galaxies, source) { nameCursor.copy(recordId = it.galaxyId.value, name = it.name.lowercase()) }
        }
        FeedType.TopGalaxies -> {
            val scoreCursor = cursor as? EntityCursor.Score ?: EntityCursor.Score.Default.copy(search = search)
            val galaxies = dao.galaxy.readTopGalaxies(callerId, scoreCursor)
            pagedFeedOf(galaxies, source) { scoreCursor.copy(recordId = it.galaxyId.value, score = it.starCount.toDouble()) }
        }
        FeedType.NewGalaxies -> {
            val timeCursor = cursor as? EntityCursor.Time ?: EntityCursor.Default.copy(search = search)
            val galaxies = dao.galaxy.readNewGalaxies(callerId, timeCursor)
            pagedFeedOf(galaxies, source) { timeCursor.copy(recordId = it.galaxyId.value, recordAt = it.createdAt) }
        }
        else -> null
    }
}

/** The city feed with the first page of its cities. */
suspend fun DaoScope.readCityFeedContent(): CityFeedContent = CityFeedContent(readCitiesFeed())

/** The page at [cursor] of every city by name. A cursor of another kind starts the first page, keeping its search. */
suspend fun DaoScope.readCitiesFeed(cursor: EntityCursor? = null): EntityFeed {
    val nameCursor = cursor as? EntityCursor.Name ?: EntityCursor.Name.Default.copy(search = cursor?.search)
    val cities = dao.city.readCities(nameCursor)
    return pagedFeedOf(cities, FeedSource(FeedContext.Cities, FeedType.Cities)) {
        nameCursor.copy(recordId = it.cityId.value, name = it.name.lowercase())
    }
}

/** A feed of [entities] from [source], continuing after the last with [nextCursor] when they fill a page. */
private fun <T : Entity> pagedFeedOf(entities: List<T>, source: FeedSource, nextCursor: (T) -> EntityCursor): EntityFeed {
    val next = entities.takeIf { it.size >= EntityCursor.DefaultLimit }?.last()?.let(nextCursor)
    return EntityFeed(entities, nextCursor = next, source = source)
}

/** The page at [cursor] of the posts of the galaxies [callerId] follows, or of every galaxy without a caller. */
suspend fun DaoScope.readHomePostFeed(callerId: CallerId?, cursor: EntityCursor = EntityCursor.Default): EntityFeed {
    val posts = dao.post.readHomePosts(callerId, cursor)
    return readFeedMarks(posts, callerId, cursor, FeedSource(FeedContext.Home, FeedType.GalaxyPosts))
}

/** The page at [cursor] of the posts of the galaxy [galaxyId], as seen by [callerId]. */
suspend fun DaoScope.readGalaxyFeed(
    galaxyId: GalaxyId,
    callerId: CallerId?,
    cursor: EntityCursor = EntityCursor.Default,
): EntityFeed {
    val posts = dao.post.readGalaxyPosts(galaxyId, callerId, cursor)
    return readFeedMarks(posts, callerId, cursor, FeedSource(FeedContext.Galaxy(galaxyId), FeedType.Posts))
}

/**
 * The page at [cursor] of the media shown in [context], newest first: the star's own in a star's context, and
 * everyone's otherwise.
 */
suspend fun DaoScope.readMediaFeed(context: FeedContext, cursor: EntityCursor = EntityCursor.Default): EntityFeed {
    val timeCursor = cursor.timeOr(EntityCursor.Default)
    val username = (context as? FeedContext.Star)?.username
    val media = dao.media.readNewestMedia(timeCursor, username)
    val nextCursor = media.takeIf { it.size >= EntityCursor.DefaultLimit }?.last()?.let {
        timeCursor.copy(recordId = it.mediaId.value, recordAt = it.createdAt)
    }
    return EntityFeed(media, nextCursor = nextCursor, source = FeedSource(context, FeedType.Media))
}

/**
 * The page of upcoming events at [cursor] as seen by [callerId], with the cursor of the next page when this one is
 * full.
 */
suspend fun DaoScope.readEventFeed(
    cursor: EntityCursor = EntityCursor.Upcoming,
    callerId: CallerId? = null,
): EntityFeed {
    val timeCursor = cursor.timeOr(EntityCursor.Upcoming)
    val events = dao.event.readUpcomingEvents(timeCursor, callerId)
    return upcomingFeedOf(events, timeCursor, FeedSource(FeedContext.Home, FeedType.Events))
}

/** The city at [slug] with the first page of its [feed] type, its upcoming events by default. */
suspend fun DaoScope.readCityContent(slug: Slug, callerId: CallerId?, feed: FeedType? = null): CityContent? {
    val city = dao.city.readCity(slug) ?: return null
    val cityFeed = when (feed) {
        FeedType.Locations -> readCityLocationFeed(city.cityId, callerId)
        else -> readCityFeed(city.cityId, callerId)
    }
    return CityContent(city = city, feed = cityFeed)
}

/**
 * The page at [cursor] of the named locations in [cityId], newest first, with the cursor of the next page when this one is
 * full.
 */
suspend fun DaoScope.readCityLocationFeed(
    cityId: CityId,
    callerId: CallerId?,
    cursor: EntityCursor = EntityCursor.Default,
): EntityFeed {
    val timeCursor = cursor.timeOr(EntityCursor.Default)
    val locations = dao.location.readCityLocations(cityId, callerId, timeCursor)
    val nextCursor = locations.takeIf { it.size >= EntityCursor.DefaultLimit }?.last()?.let {
        timeCursor.copy(recordId = it.locationId.value, recordAt = it.createdAt)
    }
    return EntityFeed(locations, nextCursor = nextCursor, source = FeedSource(FeedContext.City(cityId), FeedType.Locations))
}

/** The page of a city's upcoming events at [cursor], with the cursor of the next page when this one is full. */
suspend fun DaoScope.readCityFeed(
    cityId: CityId,
    callerId: CallerId?,
    cursor: EntityCursor = EntityCursor.Upcoming,
): EntityFeed {
    val timeCursor = cursor.timeOr(EntityCursor.Upcoming)
    val events = dao.event.readUpcomingEvents(timeCursor, callerId, cityId)
    return upcomingFeedOf(events, timeCursor, FeedSource(FeedContext.City(cityId), FeedType.Events))
}

/**
 * The page at [cursor] of the events at [locationId], soonest first, with the cursor of the next page when this one is
 * full. An event that began within [LocationEventGrace] still shows, as it may be under way.
 */
suspend fun DaoScope.readLocationEventFeed(
    locationId: LocationId,
    callerId: CallerId?,
    cursor: EntityCursor = EntityCursor.Upcoming,
): EntityFeed {
    val timeCursor = cursor.timeOr(EntityCursor.Upcoming)
    val since = Clock.System.now() - LocationEventGrace
    val events = dao.event.readUpcomingEvents(timeCursor, callerId, locationId = locationId, since = since)
    return upcomingFeedOf(events, timeCursor, FeedSource(FeedContext.Location(locationId), FeedType.Events))
}

/** How long after its start an event still shows on its location's page. */
val LocationEventGrace = 6.hours

/** This cursor when it pages by time, or [default] for a cursor of another kind. */
private fun EntityCursor.timeOr(default: EntityCursor.Time) = this as? EntityCursor.Time ?: default

/** A feed of upcoming [events] read at [cursor], continuing from the last when they fill a page. */
private fun upcomingFeedOf(events: List<EventLocation>, cursor: EntityCursor.Time, source: FeedSource): EntityFeed {
    val nextCursor = events.takeIf { it.size >= EntityCursor.DefaultLimit }?.last()?.let {
        cursor.copy(recordId = it.eventId.value, recordAt = it.startsAt)
    }
    return EntityFeed(events, nextCursor = nextCursor, source = source)
}

/** The page of map groups in the view of [query], with the cursor of the next page when this one is full. */
suspend fun DaoScope.readEarthFeed(query: MapQuery): EntityFeed {
    val groups = dao.earth.readBoundedEntities(query)
    val nextCursor = groups.takeIf { it.size >= EntityCursor.MapLimit }?.last()?.let {
        query.cursor.copy(recordId = it.locationId.value, score = it.score)
    }
    return EntityFeed(groups, nextCursor = nextCursor)
}

suspend fun DaoScope.readGalaxyContent(
    slug: Slug,
    callerId: CallerId?,
    cursor: EntityCursor = EntityCursor.Default
): GalaxyContent? {
    val galaxy = dao.galaxy.readGalaxy(slug, callerId) ?: return null
    return GalaxyContent(
        galaxy = galaxy,
        feed = readGalaxyFeed(galaxy.galaxyId, callerId, cursor)
    )
}

/** A feed of [posts] from [source] with the marks of their galaxies and the caller's marks on each post. */
suspend fun DaoScope.readFeedMarks(
    posts: List<Entity>,
    callerId: CallerId?,
    cursor: EntityCursor = EntityCursor.Default,
    source: FeedSource? = null,
): EntityFeed {
    val feedMarks = dao.galaxy.readFeedMarks(posts.mapNotNull { it.post?.galaxy?.galaxyId }.toSet())
    val postMarks = dao.post.readPostMarks(posts.mapNotNull { it.post?.postId }, callerId)
    val nextCursor = cursor.next(posts)
    return EntityFeed(posts, feedMarks, postMarks, nextCursor, source)
}

/** The cursor after [entities], or `null` when they did not fill a page. */
fun EntityCursor.next(entities: List<Entity>): EntityCursor? {
    val lastPost = entities.takeIf { it.size >= EntityCursor.DefaultLimit }?.last()?.post ?: return null
    return when (this) {
        is EntityCursor.Time -> copy(recordId = lastPost.postId.value, recordAt = lastPost.createdAt)
        is EntityCursor.Score -> copy(recordId = lastPost.postId.value, score = lastPost.lean?.toDouble() ?: 0.0)
        is EntityCursor.Mark -> copy(recordId = lastPost.postId.value, count = lastPost.markCount ?: 0)
        is EntityCursor.Name -> null
    }
}