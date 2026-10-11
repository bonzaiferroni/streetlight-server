package streetlight.server.model

import kampfire.api.Slug
import kampfire.api.Username
import klutch.db.model.Identity
import koala.model.DocId
import streetlight.model.data.DocContent
import streetlight.model.data.EventUpdaterContent
import streetlight.model.data.FeedContext
import streetlight.model.data.LocationContent
import streetlight.model.data.LocationUpdaterContent
import streetlight.model.data.StarContent
import streetlight.web.model.SiteDocTable
import streetlight.web.model.SiteDocTree

/** The page content of the location [slug]. The caller can edit it when it has no host or they are its host. */
suspend fun DaoScope.readLocationContent(slug: Slug, identity: Identity?): LocationContent? {
    val locationLayout = dao.location.readDesign(slug, identity?.callerId) ?: return null
    val location = locationLayout.location
    val canEdit =
        location.host == null || location.host == identity?.username // || callerId?.roles?.contains(UserRole.Admin) == true
    return LocationContent(
        location = location,
        feed = readLocationEventFeed(location.locationId, identity?.callerId),
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



suspend fun DaoScope.readStarContent(username: Username, caller: Identity?): StarContent? {
    val star = dao.star.readStar(username, caller?.callerId, true) ?: return null
    return StarContent(
        star = star,
        feed = readMediaFeed(FeedContext.Star(username)),
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