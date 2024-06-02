package streetlight.server.data.user

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object PerformanceTable : IntIdTable() {
    val user = reference("user_id", UserTable)
    val name = text("name")
}

class PerformanceEntity(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, PerformanceEntity>(PerformanceTable)

    var user by UserEntity referencedOn PerformanceTable.user
    var name by PerformanceTable.name
}