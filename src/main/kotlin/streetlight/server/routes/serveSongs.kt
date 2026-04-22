package streetlight.server.routes

import klutch.server.*
import streetlight.model.Api
import streetlight.model.data.toProjectId
import streetlight.server.model.*

fun StreetlightRouting.serveSongs() {
    val dao = server.dao.song
    val service = server.service.song

    getEndpoint(Api.SongProfile, { it.toProjectId() }) {
        val songId = it.data
        dao.readById(songId)
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
//        getEndpoint(Api.Songs.TakeNextSong, { it.toProjectId() }) { eventId, endpoint ->
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