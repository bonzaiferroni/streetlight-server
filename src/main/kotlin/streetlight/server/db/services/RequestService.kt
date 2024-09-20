package streetlight.server.db.services

import streetlight.model.core.Request
import streetlight.server.db.DataService
import streetlight.server.db.tables.RequestEntity
import streetlight.server.db.tables.fromData
import streetlight.server.db.tables.toData

class RequestService : DataService<Request, RequestEntity>(
    RequestEntity,
    RequestEntity::fromData,
    RequestEntity::toData
) {
}