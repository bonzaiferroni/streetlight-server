package streetlight.server.db.services

import streetlight.server.db.ApiService

class EventInfoService : ApiService() {
    suspend fun read(id: Int) = dbQuery {
        EventEntity.findById(id)?.toEventInfo()
    }

    suspend fun readAll() = dbQuery {
        EventEntity.all().map { it.toEventInfo() }
    }
}