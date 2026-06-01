package streetlight.server.db.services

import kampfire.api.Slug
import kampfire.api.isValid
import klutch.db.DbService
import klutch.db.inList
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
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
import klutch.db.tables.SlugRecord
import klutch.db.tables.getDefinedSlugRecord
import klutch.db.tables.isSlugAvailable
import streetlight.server.db.tables.toGalaxy
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.updateRecord

class GalaxyTableDao : DbService() {

    suspend fun readGalaxy(slug: Slug) = dbQuery {
        GalaxyTable.read { it.slug.eq(slug) }.firstOrNull()?.toGalaxy()
    }

    suspend fun readGalaxy(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.read { it.id.eq(galaxyId) }
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
        val slug = requireNotNull(edit.slug) { "Slug not found" }
        require(slug.isValid()) { "Invalid slug" }
        require(GalaxyTable.isSlugAvailable(slug)) { "Slug is taken" }

        GalaxyTable.insert {
            it.createRecord(edit.toGalaxy(), starId, SlugRecord(slug), city, imageSet)
        }
        slug
    }

    suspend fun update(edit: GalaxyEdit, city: City?, imageSet: SavedImageSet?) = dbQuery {
        val slug = requireNotNull(edit.slug) { "Slug not found" }
        val galaxyId = requireNotNull(edit.galaxyId) { "galaxy id not found" }
        val slugRecord = GalaxyTable.getDefinedSlugRecord(galaxyId, slug)

        val galaxy = edit.toGalaxy()
        GalaxyTable.update(where = { GalaxyTable.id.eq(galaxyId) }) {
            it.updateRecord(galaxy, slugRecord, city, imageSet)
        }
        slug
    }

    suspend fun delete(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.deleteWhere { GalaxyTable.id.eq(galaxyId) } == 1
    }
}

fun GalaxyEdit.toGalaxy() = Galaxy(
    galaxyId = galaxyId ?: GalaxyId.random(),
    cityId = cityId,
    city = null,
    name = requireNotNull(name?.trim().takeIf { GalaxyEdit.isValidName(it) }) { "invalid name: $name" },
    slug = Slug.Empty,
    tagline = tagline?.trim(),
    description = description?.trim(),
    geoPoint = requireNotNull(geoBounds?.center) { "geo bounds not found" },
    geoBounds = requireNotNull(geoBounds) { "geo bounds not found" },
    postPermission = postPermission,
    reviewMode = reviewMode,
    postGuide = postGuide?.trim(),
    imageRef = imageRef,
    images = null,
    lightCount = 0,
    eventCount = null,
    locationCount = null,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)