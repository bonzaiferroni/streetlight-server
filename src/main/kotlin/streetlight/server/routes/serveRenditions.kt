package streetlight.server.routes

import klutch.server.*
import streetlight.model.Api
import streetlight.model.data.toProjectId
import streetlight.server.model.*

fun ApiContext.serveRenditions() {
    getEndpoint(Api.RenditionFeed, { it.toProjectId() }) {
        val id = it.data
        dao.rendition.readById(id)
    }

    getEndpoint(Api.RenditionFeed.BySong, { it.toProjectId() }) {
        val songId = it.data
        dao.rendition.readAllBySongId(songId)
    }

//    authenticateJwt {
//        postEndpoint(Api.RenditionFeed.Create) {
//            val userId = getUserId()
//            dao.create(userId, it.data)
//        }
//
//        getEndpoint(Api.RenditionFeed.ReadAllSince) { endpoint ->
//            val since: Instant = readParam(endpoint.since)
//            val userId = getUserId()
//            dao.readAllSince(userId, since)
//        }
//
//        postEndpoint(Api.RenditionFeed.Update) {
//            dao.update(it.data)
//        }
//
//        deleteEndpoint(Api.RenditionFeed.Delete) { songPlayId, _ ->
//            dao.delete(songPlayId)
//        }
//    }
}
