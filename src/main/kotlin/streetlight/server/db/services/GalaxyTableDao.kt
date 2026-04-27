package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.inList
import klutch.db.read
import klutch.db.readFirstOrNull
import klutch.utils.eq
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
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

    suspend fun readGalaxyName(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.select(GalaxyTable.name).where { GalaxyTable.id.eq(galaxyId) }.firstOrNull()?.getOrNull(GalaxyTable.name)
    }

    suspend fun readGalaxyByPath(path: String) = dbQuery {
        GalaxyTable.selectAll().where { GalaxyTable.path.eq(path) }.firstOrNull()?.toGalaxy()
    }

    suspend fun readTopGalaxies(limit: Int = 10) = dbQuery {
        // .orderBy(GalaxyTable.lightCount, SortOrder.DESC)
        GalaxyTable.selectAll().limit(limit).map { it.toGalaxy() }
    }
    
    suspend fun readGalaxies(galaxyIds: List<GalaxyId>) = dbQuery {
        GalaxyTable.selectAll().where { GalaxyTable.id.inList(galaxyIds) }.map { it.toGalaxy() }
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
        GalaxyLightTable.read { GalaxyLightTable.starId.eq(starId) }.map { it[GalaxyLightTable.galaxyId].toProjectId<GalaxyId>() }
    }

    suspend fun editGalaxyLight(edit: LightEdit, starId: StarId) = dbQuery {
        when (edit.isLit) {
            true -> GalaxyLightTable.insertIgnore {
                it[this.starId] = starId.toUUID()
                it[this.galaxyId] = edit.stringId.toUUID()
                it[this.createdAt] = Clock.System.now()
            }
            else -> GalaxyLightTable.deleteWhere {
                this.galaxyId.eq(edit.stringId) and this.starId.eq(starId)
            }
        }
        true
    }

    suspend fun editGalaxyLights(edits: List<LightEdit>, starId: StarId) = dbQuery {
        val (toLight, toUnlight) = edits.partition { it.isLit }

        val existingIds = GalaxyTable
            .select(GalaxyTable.id)
            .where { GalaxyTable.id inList toLight.map { it.stringId.toUUID() } }
            .map { it[GalaxyTable.id].value }
            .toSet()

        GalaxyLightTable.batchInsert(toLight.filter { it.stringId.toUUID() in existingIds }, ignore = true) {
            this[GalaxyLightTable.starId] = starId.toUUID()
            this[GalaxyLightTable.galaxyId] = it.stringId.toUUID()
            this[GalaxyLightTable.createdAt] = Clock.System.now()
        }

        toUnlight.forEach { edit ->
            GalaxyLightTable.deleteWhere {
                this.galaxyId.eq(edit.stringId) and this.starId.eq(starId)
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
    lightCount = null,
    eventCount = null,
    locationCount = null,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)