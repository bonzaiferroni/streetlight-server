package streetlight.server.db.services

import kampfire.api.Slug
import kampfire.api.toSlug
import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.upsert
import streetlight.model.data.LocationId
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.SubdomainTable
import kotlin.time.Clock

class SubdomainTableDao: DbService() {
    suspend fun upsert(locationId: LocationId, slug: Slug) = dbQuery {
        val now = Clock.System.now()
        SubdomainTable.upsert(
            SubdomainTable.locationId,
            onUpdate = {
                it[SubdomainTable.slug] = insertValue(SubdomainTable.slug)
                it[SubdomainTable.updatedAt] = insertValue(SubdomainTable.updatedAt)
            }
        ) {
            it[SubdomainTable.locationId] = locationId.value
            it[SubdomainTable.slug] = slug.value
            it[SubdomainTable.updatedAt] = now
            it[SubdomainTable.createdAt] = now
        }.insertedCount
    }

    /** Whether [slug] is free for [locationId]: unused, or already its own. */
    suspend fun isAvailable(slug: Slug, locationId: LocationId) = dbQuery {
        SubdomainTable.select(SubdomainTable.id).where {
            SubdomainTable.slug.eq(slug) and
                (SubdomainTable.locationId.isNull() or not(SubdomainTable.locationId.eq(locationId)))
        }.limit(1).none()
    }

    suspend fun delete(locationId: LocationId) = dbQuery {
        SubdomainTable.deleteWhere { SubdomainTable.locationId.eq(locationId) }
    }

    /** The slug of the location a subdomain belongs to. */
    suspend fun readLocationSlug(subdomainSlug: Slug) = dbQuery {
        SubdomainTable.leftJoin(LocationTable).select(LocationTable.slug).where {
                SubdomainTable.slug.eq(subdomainSlug)
            }.singleOrNull()?.getOrNull(LocationTable.slug)?.toSlug()
    }
}