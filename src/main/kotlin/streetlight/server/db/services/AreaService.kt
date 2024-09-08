package streetlight.server.db.services

import streetlight.model.core.Area
import streetlight.server.db.DataService
import streetlight.server.db.tables.AreaEntity

class AreaService : DataService<Area, AreaEntity>(AreaEntity) {
    override suspend fun createEntity(data: Area): AreaEntity.() -> Unit = {
        name = data.name
    }

    override fun AreaEntity.toData() = Area(
        this.id.value,
        this.name,
    )

    override suspend fun updateEntity(data: Area): (AreaEntity) -> Unit = {
        it.name = data.name
    }
}