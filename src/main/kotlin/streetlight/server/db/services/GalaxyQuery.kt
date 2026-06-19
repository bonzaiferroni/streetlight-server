package streetlight.server.db.services

import kampfire.api.toMarkdown
import kampfire.api.toSlug
import klutch.utils.eq
import klutch.utils.toGeoBounds
import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.CityId
import streetlight.model.data.Galaxy
import streetlight.model.data.StarId
import streetlight.server.db.tables.GalaxyHostTable
import streetlight.server.db.tables.GalaxyStarTable
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.getConstraint
import streetlight.server.utils.toRecordId

fun galaxyQuery(callerId: StarId?) = GalaxyTable
    .join(GalaxyStarTable, JoinType.LEFT, GalaxyTable.id, GalaxyStarTable.galaxyId,
        additionalConstraint = GalaxyStarTable.getConstraint(callerId))
    .join(GalaxyHostTable, JoinType.LEFT, GalaxyTable.id, GalaxyHostTable.galaxyId,
        additionalConstraint = getConstraint(callerId) { GalaxyHostTable.hostId.eq(it) })
    .select(GalaxyColumns)

val GalaxyColumns = listOf(
    GalaxyTable.id,
    GalaxyTable.cityId,
    GalaxyTable.slug,
    GalaxyTable.name,
    GalaxyTable.tagline,
    GalaxyTable.description,
    GalaxyTable.city,
    GalaxyTable.geoPoint,
    GalaxyTable.geoBounds,
    GalaxyTable.postPermission,
    GalaxyTable.reviewCount,
    GalaxyTable.postGuide,
    GalaxyTable.imageRef,
    GalaxyTable.images,
    GalaxyTable.starCount,
    GalaxyTable.eventCount,
    GalaxyTable.locationCount,
    GalaxyTable.postCount,
    GalaxyTable.updatedAt,
    GalaxyTable.createdAt,
    GalaxyStarTable.starId,
    GalaxyHostTable.hostId,
)

fun ResultRow.toGalaxy() = Galaxy(
    galaxyId = toRecordId(GalaxyTable.id),
    cityId = this[GalaxyTable.cityId]?.let { CityId(it.value) },
    city = this[GalaxyTable.city],
    slug = this[GalaxyTable.slug].toSlug(),
    name = this[GalaxyTable.name],
    tagline = this[GalaxyTable.tagline],
    description = this[GalaxyTable.description],
    geoPoint = this[GalaxyTable.geoPoint].toGeoPoint(),
    geoBounds = this[GalaxyTable.geoBounds].toGeoBounds(),
    postPermission = this[GalaxyTable.postPermission],
    reviewCount = this[GalaxyTable.reviewCount],
    postGuide = this[GalaxyTable.postGuide],
    imageRef = this[GalaxyTable.imageRef],
    images = this[GalaxyTable.images],
    isLit = this.getOrNull(GalaxyStarTable.starId) != null,
    isHost = this.getOrNull(GalaxyHostTable.hostId) != null,
    starCount = this[GalaxyTable.starCount],
    eventCount = this[GalaxyTable.eventCount],
    locationCount = this[GalaxyTable.locationCount],
    postCount = this[GalaxyTable.starCount],
    updatedAt = this[GalaxyTable.updatedAt],
    createdAt = this[GalaxyTable.createdAt],
)