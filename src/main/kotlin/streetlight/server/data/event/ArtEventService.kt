package streetlight.server.data.event

import streetlight.model.ArtEvent
import streetlight.server.data.DataService
import streetlight.server.data.user.UserEntity

class ArtEventService : DataService<ArtEvent, ArtEventEntity>("art_events", ArtEventEntity) {
    override suspend fun createEntity(data: ArtEvent): (ArtEventEntity.() -> Unit)? {
        val event = EventEntity.findById(data.eventId) ?: return null
        val user = UserEntity.findById(data.userId) ?: return null
        return {
            streamUrl = data.streamUrl
            tips = data.tips
            this.user = user
            this.event = event
        }
    }

    override fun ArtEventEntity.toData() = ArtEvent(
        id.value,
        user.id.value,
        event.id.value,
        streamUrl,
        tips,
    )

}
