package streetlight.server.data.location

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import streetlight.server.data.area.AreaEntity
import streetlight.server.data.area.AreaService
import streetlight.server.data.area.AreaTable

object LocationTable : IntIdTable() {
    val name = text("name")
    val latitude = double("latitude")
    val longitude = double("longitude")
    val area = reference(
        name = "area_id",
        foreign = AreaTable,
        onDelete = ReferenceOption.CASCADE
    )
}

class Locations(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, Locations>(LocationTable)

    var name by LocationTable.name
    var latitude by LocationTable.latitude
    var longitude by LocationTable.longitude
    var area by AreaEntity referencedOn LocationTable.area
}