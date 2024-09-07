package streetlight.server.db.services

import streetlight.model.Request
import streetlight.server.db.DataService
import streetlight.server.db.tables.EventEntity
import streetlight.server.db.tables.RequestEntity
import streetlight.server.db.tables.SongEntity

class RequestService : DataService<Request, RequestEntity>(RequestEntity) {
    override suspend fun createEntity(data: Request): (RequestEntity.() -> Unit)? {
        val event = EventEntity.findById(data.eventId) ?: return null
        val song = SongEntity.findById(data.songId) ?: return null
        return {
            this.event = event
            this.song = song
            time = data.time
            performed = data.performed
            notes = data.notes
            requesterName = data.requesterName
        }
    }

    override fun RequestEntity.toData() = Request(
        id.value,
        event.id.value,
        song.id.value,
        time,
        performed,
        notes,
        requesterName
    )

    override suspend fun updateEntity(data: Request): ((RequestEntity) -> Unit)? {
        val event = EventEntity.findById(data.eventId) ?: return null
        val song = SongEntity.findById(data.songId) ?: return null
        return {
            it.event = event
            it.song = song
            it.time = data.time
            it.performed = data.performed
            it.notes = data.notes
            it.requesterName = data.requesterName
        }
    }
}