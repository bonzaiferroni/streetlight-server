package streetlight.server.db.services

import streetlight.model.core.Area
import streetlight.model.core.Event
import streetlight.model.core.Location
import streetlight.model.core.User
import streetlight.model.dto.EventInfo
import streetlight.server.db.DataService
import streetlight.server.db.tables.EventEntity
import streetlight.server.db.tables.LocationEntity
import streetlight.server.db.tables.RequestEntity
import streetlight.server.db.tables.UserEntity

class EventService : DataService<Event, EventEntity>(EventEntity) {
    override suspend fun createEntity(data: Event): (EventEntity.() -> Unit)? {
        val location = LocationEntity.findById(data.locationId) ?: return null
        val user = UserEntity.findById(data.userId) ?: return null
        val currentRequest = data.currentRequestId?.let { RequestEntity.findById(it) }
        return {
            this.user = user
            this.location = location
            timeStart = data.timeStart
            hours = data.hours
            url = data.imageUrl
            imageUrl = data.imageUrl
            streamUrl = data.streamUrl
            name = data.name
            description = data.description
            status = data.status
            this.currentRequest = currentRequest
            cashTips = data.cashTips
            cardTips = data.cardTips
        }
    }

    override fun EventEntity.toData() = this.toEvent()

    override suspend fun updateEntity(data: Event): ((EventEntity) -> Unit)? {
        val location = LocationEntity.findById(data.locationId) ?: return null
        val user = UserEntity.findById(data.userId) ?: return null
        val currentRequest = data.currentRequestId?.let { RequestEntity.findById(it) }
        return {
            it.location = location
            it.user = user
            it.timeStart = data.timeStart
            it.hours = data.hours
            it.url = data.imageUrl
            it.imageUrl = data.imageUrl
            it.streamUrl = data.streamUrl
            it.name = data.name
            it.description = data.description
            it.status = data.status
            it.currentRequest = currentRequest
            it.cashTips = data.cashTips
            it.cardTips = data.cardTips
        }
    }
}

fun EventEntity.toEvent(): Event {
    return Event(
        id = id.value,
        locationId = location.id.value,
        userId = user.id.value,
        timeStart = timeStart,
        hours = hours,
        url = url,
        imageUrl = imageUrl,
        streamUrl = streamUrl,
        name = name,
        description = description,
        status = status,
        currentRequestId = currentRequest?.id?.value,
        cashTips = cashTips,
        cardTips = cardTips
    )
}

fun EventEntity.toEventInfo(): EventInfo {
    return EventInfo(
        event = this.toEvent(),
        location = Location(
            id = location.id.value,
            name = location.name,
            latitude = location.latitude,
            longitude = location.longitude
        ),
        area = Area(
            id = location.area.id.value,
            name = location.area.name
        ),
        user = User(
            id = user.id.value,
            name = user.name,
            email = user.email
        ),
        currentRequest = currentRequest?.toRequestInfo(),
        requests = requests
            .filter { !it.performed }
            .map { it.toRequestInfo() }
    )
}