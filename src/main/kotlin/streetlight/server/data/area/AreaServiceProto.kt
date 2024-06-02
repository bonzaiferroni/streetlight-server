package streetlight.server.data.area

import streetlight.model.Area
import streetlight.server.data.ApiServiceBase

class AreaServiceProto : ApiServiceBase<Area, AreaEntity>("areas", AreaEntity) {
    override suspend fun createEntity(data: Area): AreaEntity.() -> Unit = {
        name = data.name
    }

    override fun AreaEntity.toData() = Area(
        this.id.value,
        this.name,
    )
}