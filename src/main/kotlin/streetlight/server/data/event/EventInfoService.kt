package streetlight.server.data.event

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import streetlight.dto.EventInfo
import streetlight.server.data.ApiService
import streetlight.server.data.area.AreaTable
import streetlight.server.data.location.LocationTable

class EventInfoService : ApiService() {
    suspend fun read(id: Int): EventInfo? {
        return dbQuery {
            EventTable.innerJoin(LocationTable).innerJoin(AreaTable)
                .select(eventInfoColumns)
                .where { EventTable.id eq id }
                .firstOrNull()
                ?.toEventInfo()
        }
    }

    suspend fun readAll(): List<EventInfo> {
        return dbQuery {
            EventTable.innerJoin(LocationTable).innerJoin(AreaTable)
                .select(eventInfoColumns)
                .map { it.toEventInfo() }
        }
    }
}

val eventInfoColumns = listOf(
    EventTable.id,
    LocationTable.name,
    LocationTable.id,
    EventTable.timeStart,
    EventTable.timeEnd,
    LocationTable.latitude,
    LocationTable.longitude,
    AreaTable.name,
    AreaTable.id
)

fun ResultRow.toEventInfo(): EventInfo = EventInfo(
    id = this[EventTable.id].value,
    locationName = this[LocationTable.name],
    locationId = this[LocationTable.id].value,
    timeStart = this[EventTable.timeStart],
    timeEnd = this[EventTable.timeEnd],
    latitude = this[LocationTable.latitude],
    longitude = this[LocationTable.longitude],
    areaName = this[AreaTable.name],
    areaId = this[AreaTable.id].value
)