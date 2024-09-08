package streetlight.server.db.services

import streetlight.model.core.EventStatus
import streetlight.server.db.ApiService
import streetlight.server.db.tables.EventEntity
import streetlight.server.db.tables.EventTable

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