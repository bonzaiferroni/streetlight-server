package streetlight.server.db.services

import kampfire.api.SlugOrId
import kampfire.api.StringId
import klutch.db.DbService
import klutch.db.inList
import klutch.db.read
import klutch.db.readFirstOrNull
import klutch.utils.eq
import klutch.utils.eqIgnoreCase
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.orIfNotNull
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.City
import kotlin.time.Clock
import streetlight.model.data.Galaxy
import streetlight.model.data.GalaxyEdit
import streetlight.model.data.GalaxyId
import streetlight.model.data.StarId
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.SavedImageSet
import streetlight.server.db.tables.toGalaxy
import streetlight.server.db.tables.writeGalaxyFull
import streetlight.server.db.tables.writeGalaxyUpdate
import streetlight.server.utils.toProjectId
import kotlin.uuid.Uuid

class GalaxyTableDao : DbService() {

    suspend fun readGalaxy(value: SlugOrId) = dbQuery {
        val matchId = Uuid.parseOrNull(value)?.let { GalaxyTable.id.eq(it) }
        GalaxyTable.read { it.slug.eqIgnoreCase(value).orIfNotNull(matchId) }.firstOrNull()?.toGalaxy()
    }

    suspend fun readGalaxyName(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.select(GalaxyTable.name).where { GalaxyTable.id.eq(galaxyId) }.firstOrNull()?.getOrNull(GalaxyTable.name)
    }

    suspend fun readGalaxySlug(path: String) = dbQuery {
        GalaxyTable.selectAll().where { GalaxyTable.slug.eq(path) }.firstOrNull()?.toGalaxy()
    }

    suspend fun readTopGalaxies(limit: Int = 10) = dbQuery {
        // .orderBy(GalaxyTable.lightCount, SortOrder.DESC)
        GalaxyTable.selectAll().limit(limit).map { it.toGalaxy() }
    }
    
    suspend fun readGalaxies(galaxyIds: List<GalaxyId>) = dbQuery {
        GalaxyTable.selectAll().where { GalaxyTable.id.inList(galaxyIds) }.map { it.toGalaxy() }
    }

    suspend fun create(edit: GalaxyEdit, starId: StarId, city: City?, imageSet: SavedImageSet?) = dbQuery {
        val id = GalaxyTable.insertAndGetId { it.writeGalaxyFull(edit.toGalaxy(), starId, city, imageSet) }.toProjectId<GalaxyId>()
        GalaxyTable.readFirstOrNull { it.id.eq(id) }?.toGalaxy()
    }

    suspend fun update(edit: GalaxyEdit, city: City?, imageSet: SavedImageSet?) = dbQuery {
        val galaxyId = edit.galaxyId ?: error("galaxy id not found")
        val galaxy = edit.toGalaxy()
        GalaxyTable.update(where = { GalaxyTable.id.eq(galaxyId) }) {
            it.writeGalaxyUpdate(galaxy, city, imageSet)
        }
        GalaxyTable.readFirstOrNull { it.id.eq(galaxyId) }?.toGalaxy()
    }

    suspend fun delete(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.deleteWhere { GalaxyTable.id.eq(galaxyId) } == 1
    }
}

fun GalaxyEdit.toGalaxy() = Galaxy(
    galaxyId = galaxyId ?: GalaxyId.random(),
    cityId = cityId,
    city = null,
    name = name?.trim().takeIf { GalaxyEdit.isValidName(it) } ?: error("invalid name: $name"),
    slug = slug?.trim().takeIf { GalaxyEdit.isValidPath(it) } ?: error("invalid slug: $slug"),
    tagline = tagline?.trim(),
    description = description?.trim(),
    geoPoint = geoBounds?.center ?: error("geo bounds not found"),
    geoBounds = geoBounds ?: error("geo bounds not found"),
    postPermission = postPermission,
    reviewMode = reviewMode,
    postGuide = postGuide?.trim(),
    imageRef = imageRef,
    images = null,
    lightCount = null,
    eventCount = null,
    locationCount = null,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)