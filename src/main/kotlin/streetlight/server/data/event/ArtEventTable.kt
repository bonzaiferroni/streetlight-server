package streetlight.server.data.event

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import streetlight.server.data.user.UserEntity
import streetlight.server.data.user.UserTable

object ArtEventTable : IntIdTable() {
    val user = reference("user_id", UserTable)
    val event = reference("event_id", EventTable)
    val streamUrl = text("stream_url").nullable()
    val tips = double("tips").nullable()
}

class ArtEventEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, ArtEventEntity>(ArtEventTable)

    var user by UserEntity referencedOn ArtEventTable.user
    var event by EventEntity referencedOn ArtEventTable.event
    var streamUrl by ArtEventTable.streamUrl
    var tips by ArtEventTable.tips
}