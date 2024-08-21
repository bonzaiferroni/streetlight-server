package streetlight.server.db.services

import streetlight.model.Area
import streetlight.model.Event
import streetlight.model.Location
import streetlight.model.Song
import streetlight.model.User
import streetlight.model.dto.EventInfo
import streetlight.model.dto.RequestInfo
import streetlight.server.db.DataService

class EventService : DataService<Event, EventEntity>("events", EventEntity) {
    override suspend fun createEntity(data: Event): (EventEntity.() -> Unit)? {
        val location = LocationEntity.findById(data.locationId) ?: return null
        val user = UserEntity.findById(data.userId) ?: return null
        val currentSong = data.currentSongId?.let { SongEntity.findById(it) }
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
            this.currentSong = currentSong
            cashTips = data.cashTips
            cardTips = data.cardTips
        }
    }

    override fun EventEntity.toData() = this.toEvent()

    override suspend fun updateEntity(data: Event): ((EventEntity) -> Unit)? {
        val location = LocationEntity.findById(data.locationId) ?: return null
        val user = UserEntity.findById(data.userId) ?: return null
        val currentSong = data.currentSongId?.let { SongEntity.findById(it) }
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
            it.currentSong = currentSong
            it.cashTips = data.cashTips
            it.cardTips = data.cardTips
        }
    }
}

fun EventEntity.toEvent(): Event {
    return Event(
        id.value,
        location.id.value,
        user.id.value,
        timeStart,
        hours,
        url,
        imageUrl,
        streamUrl,
        name,
        description,
        status,
        currentSong?.id?.value,
        cashTips,
        cardTips
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
        currentSong = currentSong?.let {
            Song(
                id = it.id.value,
                userId = it.user.id.value,
                name = it.name,
                artist = it.artist,
                // music = it.music
            )
        },
        requests = requests
            .filter { !it.performed }
            .map {
                RequestInfo(
                    it.id.value,
                    it.event.id.value,
                    location.name,
                    it.song.id.value,
                    it.song.name,
                    it.song.artist,
                    it.notes,
                    it.requesterName,
                    it.time,
                    it.performed
                )
            }
    )
}