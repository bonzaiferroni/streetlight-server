package streetlight.server.data.location

import streetlight.model.Location
import org.jetbrains.exposed.sql.Op
import streetlight.server.data.ApiService
import streetlight.server.data.area.AreaEntity

class LocationService : ApiService() {

    suspend fun create(location: Location): Int = dbQuery {
        val dbArea = AreaEntity.findById(location.areaId) ?: return@dbQuery -1
        Locations.new {
            name = location.name
            latitude = location.latitude
            longitude = location.longitude
            area = dbArea
        }.id.value
    }

    suspend fun read(id: Int): Location? = dbQuery {
        Locations.findById(id)
            ?.let {
                Location(
                    it.id.value,
                    it.name,
                    it.latitude,
                    it.longitude,
                    it.area.id.value
                )
            }
    }

    suspend fun readAll(): List<Location> = dbQuery {
        Locations.all().map {
            Location(
                it.id.value,
                it.name,
                it.latitude,
                it.longitude,
                it.area.id.value
            )
        }
    }

    suspend fun update(id: Int, location: Location) = dbQuery {
        Locations.findById(id)?.let {
            it.name = location.name
            it.latitude = location.latitude
            it.longitude = location.longitude
            AreaEntity.findById(location.areaId)?.let {
                    a -> it.area = a
            }
        }
    }

    suspend fun delete(id: Int) = dbQuery {
        Locations.findById(id)?.delete()
    }

    suspend fun search(search: String, count: Int): List<Location> {
        if (search.isBlank()) {
            dbQuery {
                Locations.all().take(count).map {
                    Location(
                        it.id.value,
                        it.name,
                        it.latitude,
                        it.longitude,
                        it.area.id.value
                    )
                }

            }
        }
        return dbQuery {
            Locations.find(Op.build {
                LocationTable.name like search
            }).map {
                Location(
                    it.id.value,
                    it.name,
                    it.latitude,
                    it.longitude,
                    it.area.id.value
                )
            }
        }
    }
}

