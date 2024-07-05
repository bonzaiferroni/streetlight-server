package streetlight.server.db.services

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object EventTable : IntIdTable() {
    val user = reference("user_id", UserTable)
    val location = reference("location_id", LocationTable)
    val timeStart = long("time_start")
    val hours = float("hours")
    val url = text("url").nullable()
}

class EventEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, EventEntity>(EventTable)

    var user by UserEntity referencedOn EventTable.user
    var location by LocationEntity referencedOn EventTable.location
    var timeStart by EventTable.timeStart
    var hours by EventTable.hours
    var url by EventTable.url
}