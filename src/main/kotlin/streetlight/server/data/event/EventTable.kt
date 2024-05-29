package streetlight.server.data.event

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import streetlight.server.data.location.LocationTable
import streetlight.server.data.location.LocationEntity

class EventEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, EventEntity>(EventTable)

    var location by LocationEntity referencedOn EventTable.location
    var timeStart by EventTable.timeStart
    var timeEnd by EventTable.timeEnd
}

object EventTable : IntIdTable() {
    val location = reference("location_id", LocationTable)
    val timeStart = long("time_start")
    val timeEnd = long("time_end")
}