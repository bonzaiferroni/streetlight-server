package streetlight.server.db.services

import streetlight.model.data.*
import streetlight.server.ServerProvider

class GalaxyTableService(private val app: ServerProvider) {

    suspend fun readPosts(galaxyId: GalaxyId, limit: Int = 20): GalaxyListing {
        val events = app.dao.eventPost.readPosts(galaxyId, limit).takeIf { it.isNotEmpty() }
        val locations = app.dao.locationPost.readPosts(galaxyId, limit).takeIf { it.isNotEmpty() }
        return GalaxyListing(events, locations)
    }
}