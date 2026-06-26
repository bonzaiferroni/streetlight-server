package streetlight.server.routes

import kampfire.model.toResponse
import klutch.server.*
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.server.model.*

fun ApiScope.serveSongs() {
    // val service = server.get<SongTableService>()

    getApi(Api.Songs.ReadId, { it.toRecordId() }) {
        val songId = it.data
        dao.song.readById(songId).toResponse()
    }

//    authenticateJwt {
//        getEndpoint(Api.Songs) {
//            val userId = getUserId()
//            dao.readAllByUserId(userId)
//        }
//
//        postEndpoint(Api.Songs.Create) {
//            val userId = getUserId()
//            dao.createSong(userId, it.data)
//        }
//
//        getEndpoint(Api.Songs.TakeNextSong, { it.toRecordId() }) { eventId, endpoint ->
//            val since: Instant = readParam(endpoint.since)
//            val userId = getUserId()
//            service.takeNextSong(userId, eventId, since)
//        }
//
//        postEndpoint(Api.SongProfile.Update) {
//            val userId = getUserId()
//            dao.updateSong(userId, it.data)
//        }
//    }
}