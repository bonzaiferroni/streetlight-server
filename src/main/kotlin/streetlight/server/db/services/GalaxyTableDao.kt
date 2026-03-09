package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import klutch.db.readById
import klutch.utils.eq
import klutch.utils.toUUID
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import streetlight.model.data.Galaxy
import streetlight.model.data.GalaxyId
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.toGalaxy
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.utils.toProjectId

class GalaxyTableDao : DbService() {

    suspend fun readGalaxy(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.read { it.id.eq(galaxyId) }.firstOrNull()?.toGalaxy()
    }

    suspend fun readGalaxies() = dbQuery {
        GalaxyTable.read { GalaxyTable.id.isNotNull() }.map { it.toGalaxy() }
    }

    suspend fun create(galaxy: Galaxy) = dbQuery {
        GalaxyTable.insertAndGetId { it.writeFull(galaxy) }.toProjectId<GalaxyId>()
    }

    suspend fun update(galaxy: Galaxy) = dbQuery {
        GalaxyTable.update(where = { GalaxyTable.id.eq(galaxy.galaxyId) }) {
            it.writeUpdate(galaxy)
        } == 1
    }

    suspend fun delete(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.deleteWhere { GalaxyTable.id.eq(galaxyId) } == 1
    }
}
