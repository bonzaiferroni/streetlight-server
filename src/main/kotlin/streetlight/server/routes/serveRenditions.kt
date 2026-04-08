package streetlight.server.routes

import klutch.server.*
import klutch.utils.getUserId
import streetlight.model.Api
import streetlight.model.data.toProjectId
import streetlight.server.model.*
import kotlin.time.Instant

fun StreetlightRouting.serveRenditions() {
    val dao = app.dao.rendition

    getEndpoint(Api.RenditionFeed, { it.toProjectId() }) { id, _ ->
        dao.readById(id)
    }

    getEndpoint(Api.RenditionFeed.BySong, { it.toProjectId() }) { songId, _ ->
        dao.readAllBySongId(songId)
    }

    authenticateJwt {
        postEndpoint(Api.RenditionFeed.Create) {
            val userId = getUserId()
            dao.create(userId, it.data)
        }

        getEndpoint(Api.RenditionFeed.ReadAllSince) { endpoint ->
            val since: Instant = readParam(endpoint.since)
            val userId = getUserId()
            dao.readAllSince(userId, since)
        }

        postEndpoint(Api.RenditionFeed.Update) {
            dao.update(it.data)
        }

        deleteEndpoint(Api.RenditionFeed.Delete) { songPlayId, _ ->
            dao.delete(songPlayId)
        }
    }
}
