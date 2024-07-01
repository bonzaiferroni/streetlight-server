package streetlight.server.data.services

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object RequestTable : IntIdTable() {
    val event = reference("event_id", EventTable)
    val performance = reference("performance_id", PerformanceTable)
    val time = long("time")
    val performed = bool("performed")
    val notes = text("notes")
}

class RequestEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, RequestEntity>(RequestTable)

    var event by EventEntity referencedOn RequestTable.event
    var performance by PerformanceEntity referencedOn RequestTable.performance
    var time by RequestTable.time
    var performed by RequestTable.performed
    var notes by RequestTable.notes
}