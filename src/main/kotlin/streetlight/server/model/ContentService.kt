package streetlight.server.model

import kampfire.api.Slug
import streetlight.model.data.HomeContent
import streetlight.model.data.LocationContent
import streetlight.model.data.LocationId
import streetlight.model.data.StarId

class ContentService(private val dao: DaoFacade) {

    suspend fun readHomeContent(callerId: StarId?): HomeContent {
        val posts = dao.post.readOrderedPosts(callerId)
        val galaxies = dao.galaxy.readTopGalaxies(callerId, 3)
        return HomeContent(
            galaxies = galaxies,
            posts = posts,
        )
    }

    suspend fun readLocationContent(slug: Slug, callerId: StarId?): LocationContent? {
        val location = dao.location.readLocation(slug, callerId) ?: return null
        val events = dao.event.readLocationEvents(slug, callerId)
        return LocationContent(
            location = location,
            events = events,
        )
    }
}