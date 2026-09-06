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
import streetlight.model.data.HomeContent
import streetlight.model.data.LocationContent
import streetlight.model.data.LocationUpdaterContent
import streetlight.model.data.StarContent
import streetlight.web.doc.SiteDocTable
import streetlight.web.doc.SiteDocTree

suspend fun DaoScope.readHomeContent(callerId: CallerId?): HomeContent {
    val posts = dao.post.readOrderedPosts { Op.TRUE }
    val galaxies = dao.galaxy.readTopGalaxies(callerId, 3)
    // val postMarks = dao.post.readPostMarks(posts.map { it.base.postId }, callerId)
    return HomeContent(
        galaxies = galaxies,
        posts = posts,
    )
}

suspend fun DaoScope.readLocationContent(slug: Slug, identity: Identity?): LocationContent? {
    val locationLayout = dao.location.readDesign(slug, identity?.callerId) ?: return null
    val location = locationLayout.location
    val events = dao.event.readLocationEvents(slug, identity?.callerId)
    val canEdit = location.host == null || location.host == identity?.username // || callerId?.roles?.contains(UserRole.Admin) == true
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
    val feedMarks = dao.galaxy.readFeedMarks(galaxyId)
    val posts = dao.post.readOrderedPosts(galaxyId, callerId)
    val postMarks = dao.post.readPostMarks(posts.map { it.base.postId }, callerId)
    return GalaxyContent(
        galaxy = galaxy,
        posts = posts,
        feedMarks = feedMarks,
        postMarks = postMarks,
    )
}

suspend fun DaoScope.readStarContent(username: Username, caller: Identity?): StarContent? {
    val star = dao.star.readStar(username, caller?.callerId, true) ?: return null
    val posts = dao.media.readMedia(username, caller?.callerId)
    return StarContent(
        star = star,
        posts = posts,
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