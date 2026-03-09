package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import klutch.db.readById
import klutch.db.readFirst
import klutch.db.readFirstOrNull
import klutch.utils.eq
import klutch.utils.toUUID
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import streetlight.model.data.Galaxy
import streetlight.model.data.GalaxyEdit
import streetlight.model.data.GalaxyId
import streetlight.model.data.PathId
import streetlight.model.data.pathIdFromName
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.toGalaxy
import streetlight.server.db.tables.writeGalaxyFull
import streetlight.server.db.tables.writeGalaxyUpdate
import streetlight.server.utils.toProjectId

class GalaxyTableDao : DbService() {

    suspend fun readGalaxy(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.read { it.id.eq(galaxyId) }.firstOrNull()?.toGalaxy()
    }

    suspend fun readGalaxyByPath(pathId: PathId) = dbQuery {
        GalaxyTable.readFirstOrNull { it.pathId.eq(pathId) }?.toGalaxy()
    }

    suspend fun readGalaxies() = dbQuery {
        GalaxyTable.read { GalaxyTable.id.isNotNull() }.map { it.toGalaxy() }
    }

    suspend fun create(galaxy: Galaxy) = dbQuery {
        GalaxyTable.insertAndGetId { it.writeGalaxyFull(galaxy) }.toProjectId<GalaxyId>()
    }

    suspend fun create(edit: GalaxyEdit): Galaxy? {
        val name = edit.name ?: return null
        val center = edit.center ?: return null
        val galaxy = Galaxy(
            galaxyId = GalaxyId.random(),
            pathId = pathIdFromName(name),
            name = name,
            center = center,
            imageUrl = edit.imageUrl,
            thumbUrl = edit.thumbUrl,
            updatedAt = Clock.System.now(),
            createdAt = Clock.System.now(),
        )
        create(galaxy)
        return galaxy
    }

    suspend fun update(galaxy: Galaxy) = dbQuery {
        GalaxyTable.update(where = { GalaxyTable.id.eq(galaxy.galaxyId) }) {
            it.writeGalaxyUpdate(galaxy)
        } == 1
    }

    suspend fun delete(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.deleteWhere { GalaxyTable.id.eq(galaxyId) } == 1
    }
}