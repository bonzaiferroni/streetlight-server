package streetlight.server.routes

import klutch.server.*
import klutch.utils.getUserId
import kotlinx.datetime.Instant
import streetlight.model.Api
import streetlight.model.data.toProjectId
import streetlight.server.model.*

fun StreetlightRouting.serveRenditions() {
    val dao = app.dao.rendition

    getEndpoint(Api.RenditionFeed, { it.toProjectId() }) { id, _ ->
        dao.readById(id)
    }

    getEndpoint(Api.RenditionFeed.BySong, { it.toProjectId() }) { songId, _ ->
        dao.readAllBySongId(songId)
    }

    authenticateJwt {
        postEndpoint(Api.RenditionFeed.Create) { newPlay, _ ->
            val userId = getUserId()
            dao.create(userId, newPlay)
        }

        getEndpoint(Api.RenditionFeed.ReadAllSince) { endpoint ->
            val since: Instant = readParam(endpoint.since)
            val userId = getUserId()
            dao.readAllSince(userId, since)
        }

        postEndpoint(Api.RenditionFeed.Update) { play, _ ->
            dao.update(play)
        }

        deleteEndpoint(Api.RenditionFeed.Delete) { songPlayId, _ ->
            dao.delete(songPlayId)
        }
    }
}
