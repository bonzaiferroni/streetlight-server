package streetlight.server.db.dao

import kampfire.api.Slug
import kampfire.api.isValid
import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.wrapAsExpression
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.City
import kotlin.time.Clock
import kotlin.time.Instant
import streetlight.model.data.Galaxy
import streetlight.model.data.GalaxyEdit
import streetlight.model.data.GalaxyId
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.PostTable
import klutch.db.tables.isSlugAvailable
import klutch.utils.inList
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import streetlight.model.data.HostType
import streetlight.server.db.tables.GalaxyHostTable
import streetlight.server.db.tables.MarkAspect
import streetlight.server.db.tables.GalaxyMarkTable
import streetlight.server.db.tables.GalaxyStarTable
import streetlight.server.db.tables.createGalaxy
import streetlight.server.db.tables.toGalaxyMark
import streetlight.server.db.tables.updateGalaxy
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.andIfNotNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import streetlight.model.data.EntityCursor
import streetlight.model.data.SortDirection
import streetlight.server.db.tables.afterCursor
import streetlight.server.db.tables.containsPatternOf

class GalaxyTableDao : DbService() {

    suspend fun create(edit: GalaxyEdit, callerId: CallerId, city: City?) = dbQuery {
        val slug = requireNotNull(edit.slug) { "Slug not found" }
        require(slug.isValid()) { "Invalid slug" }
        require(GalaxyTable.isSlugAvailable(slug)) { "Slug is taken" }
        val record = edit.toGalaxy()

        GalaxyTable.insert {
            it.createGalaxy(record, callerId, slug, city)
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
        GalaxyMarkTable.batchInsert(edit.marks) { mark ->
            this[GalaxyMarkTable.id] = Uuid.random()
            this[GalaxyMarkTable.galaxyId] = record.galaxyId.value
            this[GalaxyMarkTable.lean] = mark.lean
            this[GalaxyMarkTable.name] = mark.name
            this[GalaxyMarkTable.createdAt] = Clock.System.now()
        }

        slug
    }

    suspend fun update(edit: GalaxyEdit, city: City?) = dbQuery {
        val galaxyId = requireNotNull(edit.galaxyId) { "galaxy id not found" }

        val galaxy = edit.toGalaxy()
        GalaxyTable.update(where = { GalaxyTable.id.eq(galaxyId) }) {
            it.updateGalaxy(galaxy, city)
        }
        GalaxyMarkTable.batchUpsert(edit.marks, GalaxyMarkTable.id,
            onUpdate = {
                it[GalaxyMarkTable.lean] = insertValue(GalaxyMarkTable.lean)
                it[GalaxyMarkTable.name] = insertValue(GalaxyMarkTable.name)
            },
        ) { mark ->
            this[GalaxyMarkTable.id] = mark.markId.value
            this[GalaxyMarkTable.galaxyId] = galaxyId.value
            this[GalaxyMarkTable.lean] = mark.lean
            this[GalaxyMarkTable.name] = mark.name
            this[GalaxyMarkTable.createdAt] = Clock.System.now()
        }
        GalaxyTable.selectAll().where { GalaxyTable.id.eq(galaxyId) }.single()[GalaxyTable.slug].let(::Slug)
    }

    suspend fun isSlugAvailable(slug: Slug) = dbQuery {
        GalaxyTable.isSlugAvailable(slug)
    }

    suspend fun isHost(galaxyId: GalaxyId, callerId: CallerId) = dbQuery {
        GalaxyHostTable.selectAll()
            .where { GalaxyHostTable.galaxyId.eq(galaxyId) and GalaxyHostTable.hostId.eq(callerId) }
            .limit(1)
            .any()
    }

    suspend fun delete(galaxyId: GalaxyId) = dbQuery {
        GalaxyTable.deleteWhere { GalaxyTable.id.eq(galaxyId) } == 1
    }

    /** Counts the upcoming events posted to each galaxy. */
    suspend fun updateEventCounts(now: Instant) = dbQuery {
        val upcoming = PostTable.join(EventTable, JoinType.INNER, PostTable.eventId, EventTable.id)
            .select(EventTable.id.countDistinct())
            .where { PostTable.galaxyId.eq(GalaxyTable.id) and EventTable.startsAt.greater(now) }
        GalaxyTable.update({ GalaxyTable.eventCount.neq(wrapAsExpression(upcoming)) }) {
            it[GalaxyTable.eventCount] = upcoming
        }
    }

    suspend fun readGalaxy(slug: Slug, callerId: CallerId?) = dbQuery {
        galaxyQuery(callerId).where { GalaxyTable.slug.eq(slug) }.firstOrNull()?.toGalaxy()
    }

    suspend fun readFeedMarks(galaxyId: GalaxyId) = dbQuery {
        MarkAspect.queryGalaxyMarks().where { GalaxyMarkTable.galaxyId.eq(galaxyId) }
            .map { it.toGalaxyMark() }
    }

    suspend fun readFeedMarks(galaxyIds: Set<GalaxyId>) = dbQuery {
        MarkAspect.queryGalaxyMarks(true).where { GalaxyMarkTable.galaxyId.inList(galaxyIds) }
            .groupBy { GalaxyId(it[GalaxyMarkTable.galaxyId].value) }
            .mapValues { (_, rows) -> rows.map { it.toGalaxyMark() } }
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

    suspend fun readUserGalaxies(callerId: CallerId) = dbQuery {
        galaxyQuery(callerId).where { GalaxyStarTable.starId.eq(callerId) }.map { it.toGalaxy() }
    }

    /** The page at [cursor] of the galaxies [callerId] has lit, by name, holding its search. */
    suspend fun readStarGalaxies(callerId: CallerId, cursor: EntityCursor.Name) = dbQuery {
        val after = cursor.name?.let { name ->
            cursor.recordId?.let { afterCursor(GalaxyNameKey, name, GalaxyTable.id, it, cursor.direction) }
        }
        galaxyQuery(callerId)
            .where { GalaxyStarTable.starId.eq(callerId) andIfNotNull after andIfNotNull galaxyHolds(cursor.search) }
            .orderBy(GalaxyNameKey to cursor.direction.toSortOrder(), GalaxyTable.id to cursor.direction.toSortOrder())
            .limit(EntityCursor.DefaultLimit)
            .map { it.toGalaxy() }
    }

    /** The page at [cursor] of every galaxy by star count, most first, holding its search. */
    suspend fun readTopGalaxies(callerId: CallerId?, cursor: EntityCursor.Score) = dbQuery {
        val after = cursor.score?.let { score ->
            cursor.recordId?.let { afterCursor(GalaxyTable.starCount, score.toInt(), GalaxyTable.id, it, cursor.direction) }
        }
        galaxyQuery(callerId)
            .where { Op.TRUE andIfNotNull after andIfNotNull galaxyHolds(cursor.search) }
            .orderBy(GalaxyTable.starCount to cursor.direction.toSortOrder(), GalaxyTable.id to cursor.direction.toSortOrder())
            .limit(EntityCursor.DefaultLimit)
            .map { it.toGalaxy() }
    }

    /** The page at [cursor] of every galaxy by creation, newest first, holding its search. */
    suspend fun readNewGalaxies(callerId: CallerId?, cursor: EntityCursor.Time) = dbQuery {
        val after = cursor.recordAt?.let { recordAt ->
            cursor.recordId?.let { afterCursor(GalaxyTable.createdAt, recordAt, GalaxyTable.id, it, cursor.direction) }
        }
        galaxyQuery(callerId)
            .where { Op.TRUE andIfNotNull after andIfNotNull galaxyHolds(cursor.search) }
            .orderBy(GalaxyTable.createdAt to cursor.direction.toSortOrder(), GalaxyTable.id to cursor.direction.toSortOrder())
            .limit(EntityCursor.DefaultLimit)
            .map { it.toGalaxy() }
    }
}

/** The key galaxies are sorted by name with, ignoring case. */
val GalaxyNameKey = GalaxyTable.name.lowerCase()

/** The condition that a galaxy's name or tagline holds [search], or `null` without a search. */
private fun galaxyHolds(search: String?) = containsPatternOf(search)?.let { pattern ->
    GalaxyTable.name.lowerCase().like(pattern) or GalaxyTable.tagline.lowerCase().like(pattern)
}

/** The sort order of this direction. */
fun SortDirection.toSortOrder() = when (this) {
    SortDirection.Ascending -> SortOrder.ASC
    SortDirection.Descending -> SortOrder.DESC
}

fun GalaxyEdit.toGalaxy() = Galaxy(
    galaxyId = galaxyId ?: GalaxyId.random(),
    cityId = cityId,
    city = null,
    name = requireNotNull(name?.trim().takeIf { GalaxyEdit.isValidName(it) }) { "invalid name: $name" },
    slug = Slug.Empty,
    tagline = tagline?.trim(),
    description = description,
    geoPoint = requireNotNull(geoRect?.center) { "geo bounds not found" },
    geoRect = requireNotNull(geoRect) { "geo bounds not found" },
    postPermission = postPermission,
    reviewCount = requireNotNull(reviewCount) { "review count not found" },
    postGuide = postGuide,
    image = image,
    design = design,
    starCount = 0,
    eventCount = 0,
    locationCount = 0,
    postCount = 0,
    isLit = false,  // provided by join
    isHost = false, // provided by join
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)