package streetlight.server.data.location

import streetlight.model.Location
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.lowerCase
import streetlight.server.data.DataService
import streetlight.server.data.area.AreaEntity

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

    override fun getSearchOp(search: String): Op<Boolean> = Op.build {
        LocationTable.name.lowerCase() like "${search.lowercase()}%"
    }
}

