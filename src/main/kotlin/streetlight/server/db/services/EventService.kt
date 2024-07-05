package streetlight.server.db.services

import streetlight.model.Event
import streetlight.server.db.DataService

class EventService : DataService<Event, EventEntity>("events", EventEntity) {
    override suspend fun createEntity(data: Event): (EventEntity.() -> Unit)? {
        val location = LocationEntity.findById(data.locationId) ?: return null
        val user = UserEntity.findById(data.userId) ?: return null
        return {
            this.user = user
            this.location = location
            timeStart = data.timeStart
            hours = data.hours
            url = data.url
        }
    }

    override fun EventEntity.toData(): Event {
        return Event(
            id.value,
            location.id.value,
            user.id.value,
            timeStart,
            hours,
            url
        )
    }

    override suspend fun updateEntity(data: Event): ((EventEntity) -> Unit)? {
        val location = LocationEntity.findById(data.locationId) ?: return null
        val user = UserEntity.findById(data.userId) ?: return null
        return {
            it.location = location
            it.user = user
            it.timeStart = data.timeStart
            it.hours = data.hours
            it.url = data.url
        }
    }
}

