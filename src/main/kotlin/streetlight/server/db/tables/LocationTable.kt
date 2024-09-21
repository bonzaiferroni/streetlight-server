package streetlight.server.db.tables

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.json.json
import streetlight.model.core.GeoPoint
import streetlight.model.core.Location
import streetlight.model.enums.ResourceType

object LocationTable : IntIdTable() {
    val userId = reference(
        name = "user_id",
        foreign = UserTable,
        onDelete = ReferenceOption.SET_NULL
    ).nullable()
    val areaId = reference(
        name = "area_id",
        foreign = AreaTable,
        onDelete = ReferenceOption.SET_NULL
    ).nullable()
    val name = text("name").nullable()
    val description = text("description").nullable()
    val address = text("address").nullable()
    val notes = text("notes").nullable()
    val latitude = double("latitude")
    val longitude = double("longitude")
    val types = json<Array<ResourceType>>("type", format)
}

val format = Json { prettyPrint = true }

class LocationEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, LocationEntity>(LocationTable)

    var user by UserEntity optionalReferencedOn LocationTable.userId
    var area by AreaEntity optionalReferencedOn LocationTable.areaId
    var name by LocationTable.name
    var description by LocationTable.description
    var address by LocationTable.address
    var notes by LocationTable.notes
    var latitude by LocationTable.latitude
    var longitude by LocationTable.longitude
    var types by LocationTable.types
}

fun LocationEntity.toData() = Location(
    this.id.value,
    this.user?.id?.value,
    this.area?.id?.value,
    this.name,
    this.description,
    this.address,
    this.notes,
    GeoPoint(this.latitude, this.longitude),
    this.types.toSet(),
)

fun LocationEntity.fromData(data: Location) {
    user = data.userId?.let { UserEntity[it] }
    area = data.areaId?.let { AreaEntity[it] }
    name = data.name
    description = data.description
    address = data.address
    notes = data.notes
    latitude = data.geoPoint.latitude
    longitude = data.geoPoint.longitude
    types = data.types.toTypedArray()
}