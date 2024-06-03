package streetlight.server.data.user

import streetlight.model.Performance
import streetlight.server.data.DataService

class PerformanceService : DataService<Performance, PerformanceEntity>("performances", PerformanceEntity) {
    override suspend fun createEntity(data: Performance): (PerformanceEntity.() -> Unit)? {
        val user = UserEntity.findById(data.userId) ?: return null
        return {
            this.user = user
            name = data.name
        }
    }

    override fun PerformanceEntity.toData() = Performance(
        id.value,
        user.id.value,
        name
    )
}