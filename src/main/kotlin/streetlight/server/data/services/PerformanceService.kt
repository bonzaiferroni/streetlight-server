package streetlight.server.data.services

import streetlight.model.Performance
import streetlight.server.data.DataService

class PerformanceService : DataService<Performance, PerformanceEntity>("performances", PerformanceEntity) {
    override suspend fun createEntity(data: Performance): (PerformanceEntity.() -> Unit)? {
        val user = UserEntity.findById(data.userId) ?: return null
        return {
            this.user = user
            name = data.name
            artist = data.artist
        }
    }

    override fun PerformanceEntity.toData() = Performance(
        id.value,
        user.id.value,
        name,
        artist
    )

    override suspend fun updateEntity(data: Performance): ((PerformanceEntity) -> Unit)? {
        val user = UserEntity.findById(data.userId) ?: return null
        return {
            it.user = user
            it.name = data.name
            it.artist = data.artist
        }
    }
}