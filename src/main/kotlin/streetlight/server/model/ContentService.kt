package streetlight.server.model

import kampfire.api.Slug
import kampfire.model.CallerId
import kampfire.model.Identity
import streetlight.model.data.EventUpdaterContent
import streetlight.model.data.GalaxyContent
import streetlight.model.data.HomeContent
import streetlight.model.data.LocationContent
import streetlight.model.data.LocationUpdaterContent
import streetlight.model.data.StarId
import streetlight.model.data.starId

suspend fun DaoScope.readHomeContent(callerId: CallerId?): HomeContent {
    val posts = dao.post.readOrderedPosts(callerId)
    val galaxies = dao.galaxy.readTopGalaxies(callerId, 3)
    return HomeContent(
        galaxies = galaxies,
        posts = posts,
    )
}

suspend fun DaoScope.readLocationContent(slug: Slug, identity: Identity?): LocationContent? {
    val location = dao.location.readLocation(slug, identity?.callerId) ?: return null
    val events = dao.event.readLocationEvents(slug, identity?.callerId)
    val canEdit = location.host == null || location.host == identity?.username // || callerId?.roles?.contains(UserRole.Admin) == true
    return LocationContent(
        location = location,
        events = events,
        canEdit = canEdit,
    )
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
    val posts = dao.post.readOrderedPosts(galaxyId, callerId)
    return GalaxyContent(
        galaxy = galaxy,
        posts = posts,
    )
}