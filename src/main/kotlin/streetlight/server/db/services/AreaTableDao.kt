package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.readAll
import klutch.utils.toStringId
import org.jetbrains.exposed.sql.insertAndGetId
import streetlight.model.data.GalaxyId
import streetlight.model.data.NewCommunity
import streetlight.model.data.Galaxy
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.AreaTable
import streetlight.server.db.tables.toArea
import streetlight.server.db.tables.writeFull

class AreaTableDao: DbService() {
    suspend fun readAreas() = dbQuery {
        AreaTable.readAll().map { it.toArea() }
    }

    suspend fun create(newCommunity: NewCommunity): GalaxyId = dbQuery {
        AreaTable.insertAndGetId {
            it.writeFull(
                Galaxy(
                    galaxyId = GalaxyId.random(),
                    name = newCommunity.name,
                    points = emptyList(),
                    communityType = newCommunity.communityType
                )
            )
        }.value.toStringId().toProjectId()
    }
}