package streetlight.server.db.services

import streetlight.model.Location
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.lowerCase
import streetlight.server.db.DataService

class LocationService : DataService<Location, LocationEntity>("locations", LocationEntity) {

    override suspend fun createEntity(data: Location): (LocationEntity.() -> Unit)? {
        val area = AreaEntity.findById(data.areaId) ?: return null
        return {
            name = data.name
            latitude = data.latitude
            longitude = data.longitude
            this.area = area
        }
    }

    override fun LocationEntity.toData() = Location(
        id.value,
        name,
        latitude,
        longitude,
        area.id.value
    )

    override suspend fun updateEntity(data: Location): ((LocationEntity) -> Unit)? {
        val area = AreaEntity.findById(data.areaId) ?: return null
        return {
            it.name = data.name
            it.latitude = data.latitude
            it.longitude = data.longitude
            it.area = area
        }
    }

    override fun getSearchOp(search: String): Op<Boolean> = Op.build {
        LocationTable.name.lowerCase() like "${search.lowercase()}%"
    }
}

