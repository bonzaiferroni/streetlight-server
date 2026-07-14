package streetlight.server.db.services

import kampfire.api.Slug
import kampfire.api.isValid
import kampfire.model.CallerId
import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.City
import kotlin.time.Clock
import streetlight.model.data.Galaxy
import streetlight.model.data.GalaxyEdit
import streetlight.model.data.GalaxyId
import streetlight.server.db.tables.GalaxyTable
import klutch.db.tables.SlugRecord
import klutch.db.tables.getDefinedSlugRecord
import klutch.db.tables.isSlugAvailable
import klutch.utils.inList
import org.jetbrains.exposed.v1.core.SortOrder
import streetlight.model.data.HostType
import streetlight.server.db.tables.GalaxyHostTable
import streetlight.server.db.tables.GalaxyStarTable
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.updateRecord

class GalaxyTableDao : DbService() {

    suspend fun create(edit: GalaxyEdit, callerId: CallerId, city: City?) = dbQuery {
        val slug = requireNotNull(edit.slug) { "Slug not found" }
        require(slug.isValid()) { "Invalid slug" }
        require(GalaxyTable.isSlugAvailable(slug)) { "Slug is taken" }
        val record = edit.toGalaxy()

        GalaxyTable.insert {
            it.createRecord(record, callerId, SlugRecord(slug), city)
        }
        GalaxyStarTable.insert {
            it[GalaxyStarTable.galaxyId] = record.galaxyId.value
            it[GalaxyStarTable.starId] = callerId.value
            it[GalaxyStarTable.createdAt] = Clock.System.now()
        }
        GalaxyHostTable.insert {
            it[GalaxyHostTable.galaxyId] = record.galaxyId.value
            it[GalaxyHostTable.hostId] = callerId.value
            it[GalaxyHostTable.hostType] = HostType.Creator
            it[GalaxyHostTable.createdAt] = Clock.System.now()
        }

        slug
    }

    suspend fun update(edit: GalaxyEdit, city: City?) = dbQuery {
        val slug = requireNotNull(edit.slug) { "Slug not found" }
        val galaxyId = requireNotNull(edit.galaxyId) { "galaxy id not found" }
        val slugRecord = GalaxyTable.getDefinedSlugRecord(galaxyId, slug)

        val galaxy = edit.toGalaxy()
        GalaxyTable.update(where = { GalaxyTable.id.eq(galaxyId) }) {
            it.updateRecord(galaxy, slugRecord, city)
        }
        slug
    }

    suspend fun delete(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.deleteWhere { GalaxyTable.id.eq(galaxyId) } == 1
    }

    suspend fun readGalaxy(slug: Slug, callerId: CallerId?) = dbQuery {
        galaxyQuery(callerId).where { GalaxyTable.slug.eq(slug) }.firstOrNull()?.toGalaxy()
    }

    suspend fun readGalaxy(galaxyId: GalaxyId, callerId: CallerId?) = dbQuery {
        galaxyQuery(callerId).where { GalaxyTable.id.eq(galaxyId) }.firstOrNull()?.toGalaxy()
    }

    suspend fun readTopGalaxies(callerId: CallerId?, limit: Int = 10) = dbQuery {
        galaxyQuery(callerId).orderBy(GalaxyTable.starCount, SortOrder.DESC)
            .limit(limit).map { it.toGalaxy() }
    }
    
    suspend fun readGalaxies(galaxyIds: List<GalaxyId>, callerId: CallerId?) = dbQuery {
        galaxyQuery(callerId).where { GalaxyTable.id.inList(galaxyIds) }.map { it.toGalaxy() }
    }

    suspend fun readGalaxies(callerId: CallerId) = dbQuery {
        galaxyQuery(callerId).where { GalaxyStarTable.starId.eq(callerId) }.map { it.toGalaxy() }
    }
}

fun GalaxyEdit.toGalaxy() = Galaxy(
    galaxyId = galaxyId ?: GalaxyId.random(),
    cityId = cityId,
    city = null,
    name = requireNotNull(name?.trim().takeIf { GalaxyEdit.isValidName(it) }) { "invalid name: $name" },
    slug = Slug.Empty,
    tagline = tagline?.trim(),
    description = description,
    geoPoint = requireNotNull(geoBounds?.center) { "geo bounds not found" },
    geoBounds = requireNotNull(geoBounds) { "geo bounds not found" },
    postPermission = postPermission,
    reviewCount = requireNotNull(reviewCount) { "review count not found" },
    postGuide = postGuide,
    image = image,
    starCount = 0,
    eventCount = 0,
    locationCount = 0,
    postCount = 0,
    isLit = false,  // provided by join
    isHost = false, // provided by join
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)