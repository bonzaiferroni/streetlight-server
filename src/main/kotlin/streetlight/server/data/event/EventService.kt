package streetlight.server.data.event

import streetlight.model.Event
import streetlight.server.data.DataService
import streetlight.server.data.location.LocationEntity

class EventService : DataService<Event, EventEntity>("events", EventEntity) {
    override suspend fun createEntity(data: Event): (EventEntity.() -> Unit)? {
        val location = LocationEntity.findById(data.locationId) ?: return null
        return {
            this.location = location
            timeStart = data.timeStart
            hours = data.hours
        }
    }

    override fun EventEntity.toData(): Event {
        return Event(
            id.value,
            location.id.value,
            timeStart,
            hours
        )
    }

    override suspend fun updateEntity(data: Event): ((EventEntity) -> Unit)? {
        val location = LocationEntity.findById(data.locationId) ?: return null
        return {
            it.location = location
            it.timeStart = data.timeStart
            it.hours = data.hours
        }
    }
}

