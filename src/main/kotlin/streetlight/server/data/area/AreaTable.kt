package streetlight.server.data.area

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object AreaTable : IntIdTable() {
    val name = text("name")
}

class AreaEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, AreaEntity>(AreaTable)

    var name by AreaTable.name
}