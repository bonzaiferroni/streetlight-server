package streetlight.server.db.tables

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import streetlight.model.core.Area

object AreaTable : IntIdTable() {
    val name = text("name")
}

class AreaEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, AreaEntity>(AreaTable)

    var name by AreaTable.name
}

fun AreaEntity.toData() = Area(
    this.id.value,
    this.name,
)

fun AreaEntity.fromData(data: Area) {
    name = data.name
}