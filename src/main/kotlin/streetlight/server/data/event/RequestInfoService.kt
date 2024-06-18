package streetlight.server.data.event

import org.jetbrains.exposed.sql.ResultRow
import streetlight.dto.RequestInfo
import streetlight.server.data.ApiService
import streetlight.server.data.location.LocationTable
import streetlight.server.data.user.PerformanceTable

class RequestInfoService : ApiService() {
    suspend fun read(id: Int): RequestInfo? {
        return dbQuery {
            RequestTable.innerJoin(EventTable).innerJoin(LocationTable).innerJoin(PerformanceTable)
                .select(requestInfoColumns)
                .where { RequestTable.id eq id }
                .firstOrNull()
                ?.toRequestInfo()
        }
    }

    suspend fun readAll(): List<RequestInfo> {
        return dbQuery {
            RequestTable.innerJoin(EventTable).innerJoin(LocationTable).innerJoin(PerformanceTable)
                .select(requestInfoColumns)
                .map { it.toRequestInfo() }
        }
    }

    suspend fun readAllByEvent(eventId: Int): List<RequestInfo> {
        return dbQuery {
            RequestTable.innerJoin(EventTable).innerJoin(LocationTable).innerJoin(PerformanceTable)
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
    PerformanceTable.id,
    PerformanceTable.name,
    RequestTable.time,
    RequestTable.performed
)

fun ResultRow.toRequestInfo(): RequestInfo = RequestInfo(
    id = this[RequestTable.id].value,
    eventId = this[EventTable.id].value,
    locationName = this[LocationTable.name],
    performanceId = this[PerformanceTable.id].value,
    performanceName = this[PerformanceTable.name],
    time = this[RequestTable.time],
    performed = this[RequestTable.performed]
)