package streetlight.server.db.services

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object PerformanceTable : IntIdTable() {
    val user = reference("user_id", UserTable)
    val name = text("name")
    val artist = text("artist").nullable()
}

class PerformanceEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, PerformanceEntity>(PerformanceTable)

    var user by UserEntity referencedOn PerformanceTable.user
    var name by PerformanceTable.name
    var artist by PerformanceTable.artist
}