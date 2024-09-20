package streetlight.server.db.services

import streetlight.model.core.Area
import streetlight.server.db.DataService
import streetlight.server.db.tables.AreaEntity
import streetlight.server.db.tables.fromData
import streetlight.server.db.tables.toData

class AreaService : DataService<Area, AreaEntity>(
    AreaEntity,
    AreaEntity::fromData,
    AreaEntity::toData
) {
}