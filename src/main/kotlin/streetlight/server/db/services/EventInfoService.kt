package streetlight.server.db.services

import io.ktor.events.Events
import streetlight.model.EventStatus
import streetlight.server.db.ApiService

class EventInfoService : ApiService() {
    suspend fun read(id: Int) = dbQuery {
        EventEntity.findById(id)?.toEventInfo()
    }

    suspend fun readAll() = dbQuery {
        EventEntity.all().map { it.toEventInfo() }
    }

    suspend fun readAllCurrent() = dbQuery {
        EventEntity.find { EventTable.status neq EventStatus.Finished }
            .map { it.toEventInfo() }
    }
}