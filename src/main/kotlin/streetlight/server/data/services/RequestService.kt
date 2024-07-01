package streetlight.server.data.services

import streetlight.model.Request
import streetlight.server.data.DataService

class RequestService : DataService<Request, RequestEntity>("requests", RequestEntity) {
    override suspend fun createEntity(data: Request): (RequestEntity.() -> Unit)? {
        val event = EventEntity.findById(data.eventId) ?: return null
        val performance = PerformanceEntity.findById(data.performanceId) ?: return null
        return {
            this.event = event
            this.performance = performance
            time = data.time
            performed = data.performed
            notes = data.notes
        }
    }

    override fun RequestEntity.toData() = Request(
        id.value,
        event.id.value,
        performance.id.value,
        time,
        performed,
        notes,
    )

    override suspend fun updateEntity(data: Request): ((RequestEntity) -> Unit)? {
        val event = EventEntity.findById(data.eventId) ?: return null
        val performance = PerformanceEntity.findById(data.performanceId) ?: return null
        return {
            it.event = event
            it.performance = performance
            it.time = data.time
            it.performed = data.performed
            it.notes = data.notes
        }
    }
}