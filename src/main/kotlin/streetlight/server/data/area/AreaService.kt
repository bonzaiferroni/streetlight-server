package streetlight.server.data.area

import streetlight.model.Area
import streetlight.server.data.ApiService
import streetlight.server.data.ApiServiceBase


class AreaService : ApiService() {

    suspend fun create(area: Area): Int = dbQuery {
        AreaEntity.new {
            name = area.name
        }.id.value
    }

    suspend fun read(id: Int): Area? {
        return dbQuery {
            AreaEntity.findById(id)
                ?.let {
                    Area(
                        it.id.value,
                        it.name,
                    )
                }
        }
    }

    suspend fun readAll(): List<Area> {
        return dbQuery {
            AreaEntity.all().map {
                Area(
                    it.id.value,
                    it.name,
                )
            }
        }
    }

    suspend fun update(id: Int, area: Area) {
        dbQuery {
            AreaEntity.findById(id)?.let {
                it.name = area.name
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            AreaEntity.findById(id)?.delete()
        }
    }
}

