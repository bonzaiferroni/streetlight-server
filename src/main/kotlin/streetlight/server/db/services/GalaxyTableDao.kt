package streetlight.server.db.services

import kampfire.model.UserId
import klutch.db.DbService
import klutch.db.inList
import klutch.db.read
import klutch.db.readFirstOrNull
import klutch.utils.eq
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import streetlight.model.data.Galaxy
import streetlight.model.data.GalaxyEdit
import streetlight.model.data.GalaxyId
import streetlight.model.data.LightEdit
import streetlight.model.data.StarId
import streetlight.model.data.slugOf
import streetlight.server.db.tables.GalaxyLightTable
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.SavedImageSet
import streetlight.server.db.tables.toGalaxy
import streetlight.server.db.tables.writeGalaxyFull
import streetlight.server.db.tables.writeGalaxyUpdate
import streetlight.server.utils.toProjectId

class GalaxyTableDao : DbService() {

    suspend fun readGalaxy(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.read { it.id.eq(galaxyId) }.firstOrNull()?.toGalaxy()
    }

    suspend fun readGalaxyByPath(path: String) = dbQuery {
        GalaxyTable.readFirstOrNull { it.path.eq(path) }?.toGalaxy()
    }

    suspend fun readTopGalaxies() = dbQuery {
        GalaxyTable.read { GalaxyTable.id.isNotNull() }.map { it.toGalaxy() }
    }
    
    suspend fun readGalaxies(galaxyIds: List<GalaxyId>) = dbQuery {
        GalaxyTable.read { GalaxyTable.id.inList(galaxyIds) }.map { it.toGalaxy() }
    }

    suspend fun create(edit: GalaxyEdit, starId: StarId, imageSet: SavedImageSet?) = dbQuery {
        val id = GalaxyTable.insertAndGetId { it.writeGalaxyFull(edit.toGalaxy(), starId, imageSet) }.toProjectId<GalaxyId>()
        GalaxyTable.readFirstOrNull { it.id.eq(id) }?.toGalaxy()
    }

    suspend fun update(galaxy: Galaxy, imageSet: SavedImageSet?) = dbQuery {
        GalaxyTable.update(where = { GalaxyTable.id.eq(galaxy.galaxyId) }) {
            it.writeGalaxyUpdate(galaxy, imageSet)
        } == 1
    }

    suspend fun delete(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.deleteWhere { GalaxyTable.id.eq(galaxyId) } == 1
    }

    suspend fun readGalaxyLights(starId: StarId) = dbQuery {
        GalaxyLightTable.read { GalaxyLightTable.StarId.eq(starId) }.map { it[GalaxyLightTable.GalaxyId].toProjectId<GalaxyId>() }
    }

    suspend fun editGalaxyLight(edit: LightEdit, starId: StarId) = dbQuery {
        when (edit.isLit) {
            true -> GalaxyLightTable.insertIgnore {
                it[this.StarId] = starId.toUUID()
                it[this.GalaxyId] = edit.stringId.toUUID()
                it[this.CreatedAt] = Clock.System.now()
            }
            else -> GalaxyLightTable.deleteWhere {
                this.GalaxyId.eq(edit.stringId) and this.StarId.eq(starId)
            }
        }
        true
    }
}

fun GalaxyEdit.toGalaxy() = Galaxy(
    galaxyId = GalaxyId.random(),
    name = name ?: error("name not found"),
    slug = slug ?: slugOf(name ?: error("name not found")),
    description = description,
    center = center ?: error("center not found"),
    zoom = zoom ?: 10f,
    postPermission = postPermission,
    reviewMode = reviewMode,
    postGuide = postGuide,
    imageRef = imageRef,
    images = null,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)