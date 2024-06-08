package streetlight.server.data.event

import streetlight.model.Request
import streetlight.server.data.DataService
import streetlight.server.data.user.PerformanceEntity

class RequestService : DataService<Request, RequestEntity>("requests", RequestEntity) {
    override suspend fun createEntity(data: Request): (RequestEntity.() -> Unit)? {
        val event = EventEntity.findById(data.eventId) ?: return null
        val performance = PerformanceEntity.findById(data.performanceId) ?: return null
        return {
            this.event = event
            this.performance = performance
            time = data.time
        }
    }

    override fun RequestEntity.toData() = Request(
        id.value,
        event.id.value,
        performance.id.value,
        time,
    )

    override suspend fun updateEntity(data: Request): ((RequestEntity) -> Unit)? {
        val event = EventEntity.findById(data.eventId) ?: return null
        val performance = PerformanceEntity.findById(data.performanceId) ?: return null
        return {
            it.event = event
            it.performance = performance
            it.time = data.time
        }
    }
}