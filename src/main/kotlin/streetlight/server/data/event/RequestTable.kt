package streetlight.server.data.event

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import streetlight.server.data.location.LocationTable
import streetlight.server.data.location.LocationEntity
import streetlight.server.data.user.PerformanceEntity
import streetlight.server.data.user.PerformanceTable

object RequestTable : IntIdTable() {
    val event = reference("event_id", EventTable)
    val performance = reference("performance_id", PerformanceTable)
    val time = long("time")
    val performed = bool("performed")
}

class RequestEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, RequestEntity>(RequestTable)

    var event by EventEntity referencedOn RequestTable.event
    var performance by PerformanceEntity referencedOn RequestTable.performance
    var time by RequestTable.time
    var performed by RequestTable.performed
}