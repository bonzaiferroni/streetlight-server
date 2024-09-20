package streetlight.server.db.services

import streetlight.model.core.Event
import streetlight.model.dto.EventInfo
import streetlight.server.db.DataService
import streetlight.server.db.tables.EventEntity
import streetlight.server.db.tables.fromData
import streetlight.server.db.tables.toData
import streetlight.server.db.tables.toInfo

class EventService : DataService<Event, EventEntity>(
    EventEntity,
    EventEntity::fromData,
    EventEntity::toData
) {
}