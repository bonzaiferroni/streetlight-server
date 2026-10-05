package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.model.CallerId
import streetlight.model.data.MapQuery

class EarthDao: DbService() {
    suspend fun readBoundedEntities(callerId: CallerId?, query: MapQuery) = dbQuery {

    }
}