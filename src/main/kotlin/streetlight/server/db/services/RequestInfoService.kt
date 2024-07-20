package streetlight.server.db.services

import org.jetbrains.exposed.sql.ResultRow
import streetlight.model.dto.RequestInfo
import streetlight.server.db.ApiService

class RequestInfoService : ApiService() {
    suspend fun read(id: Int): RequestInfo? {
        return dbQuery {
            RequestTable.innerJoin(EventTable).innerJoin(LocationTable).innerJoin(SongTable)
                .select(requestInfoColumns)
                .where { RequestTable.id eq id }
                .firstOrNull()
                ?.toRequestInfo()
        }
    }

    suspend fun readAll(): List<RequestInfo> {
        return dbQuery {
            RequestTable.innerJoin(EventTable).innerJoin(LocationTable).innerJoin(SongTable)
                .select(requestInfoColumns)
                .map { it.toRequestInfo() }
        }
    }

    suspend fun readAllByEvent(eventId: Int): List<RequestInfo> {
        return dbQuery {
            RequestTable.innerJoin(EventTable).innerJoin(LocationTable).innerJoin(SongTable)
                .select(requestInfoColumns)
                .where { EventTable.id eq eventId }
                .map { it.toRequestInfo() }
        }
    }
}

val requestInfoColumns = listOf(
    RequestTable.id,
    EventTable.id,
    LocationTable.name,
    SongTable.id,
    SongTable.name,
    SongTable.artist,
    RequestTable.notes,
    RequestTable.time,
    RequestTable.performed
)

fun ResultRow.toRequestInfo(): RequestInfo = RequestInfo(
    id = this[RequestTable.id].value,
    eventId = this[EventTable.id].value,
    locationName = this[LocationTable.name],
    songId = this[SongTable.id].value,
    songName = this[SongTable.name],
    artist = this[SongTable.artist],
    notes = this[RequestTable.notes],
    time = this[RequestTable.time],
    performed = this[RequestTable.performed],

)