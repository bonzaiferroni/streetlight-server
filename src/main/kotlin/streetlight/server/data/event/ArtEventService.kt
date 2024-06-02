package streetlight.server.data.event

import streetlight.model.ArtEvent
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.lowerCase
import streetlight.server.data.ApiService
import streetlight.server.data.area.AreaEntity
import streetlight.server.data.user.UserEntity

class ArtEventService : ApiService() {

    suspend fun create(artEvent: ArtEvent): Int = dbQuery {
        val event = EventEntity.findById(artEvent.eventId) ?: return@dbQuery -1
        val user = UserEntity.findById(artEvent.userId) ?: return@dbQuery -1
        ArtEventEntity.new {
            streamUrl = artEvent.streamUrl
            tips = artEvent.tips
            this.user = user
            this.event = event
        }.id.value
    }

    suspend fun read(id: Int): ArtEvent? = dbQuery {
        ArtEventEntity.findById(id)?.toArtEvent()
    }

    suspend fun readAll(): List<ArtEvent> = dbQuery {
        ArtEventEntity.all().map { it.toArtEvent() }
    }

    suspend fun update(id: Int, artEvent: ArtEvent) = dbQuery {
        val event = EventEntity.findById(artEvent.eventId) ?: return@dbQuery
        val user = UserEntity.findById(artEvent.userId) ?: return@dbQuery
        ArtEventEntity.findById(id)?.let {
            it.streamUrl = artEvent.streamUrl
            it.tips = artEvent.tips
            it.event = event
            it.user = user
        }
    }

    suspend fun delete(id: Int) = dbQuery {
        ArtEventEntity.findById(id)?.delete()
    }
}

fun ArtEventEntity.toArtEvent() = ArtEvent(
    id.value,
    user.id.value,
    event.id.value,
    streamUrl,
    tips,
)
