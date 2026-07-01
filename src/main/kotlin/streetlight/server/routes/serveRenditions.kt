package streetlight.server.routes

import kampfire.model.toOutcome
import klutch.server.*
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.server.model.*

fun ApiScope.serveRenditions() {
    getApi(Api.RenditionFeed, { it.toRecordId() }) {
        val id = it.data
        dao.rendition.readById(id).toOutcome()
    }

    getApi(Api.RenditionFeed.BySong, { it.toRecordId() }) {
        val songId = it.data
        dao.rendition.readAllBySongId(songId).toOutcome()
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
