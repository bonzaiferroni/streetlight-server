package streetlight.server.db.tables

import kampfire.api.toSlug
import kampfire.api.toUsername
import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.CityId
import streetlight.model.data.Location
import streetlight.model.data.ResourceType
import streetlight.model.data.StarId
import streetlight.server.utils.toRecordId

fun locationQuery(callerId: StarId?) = LocationTable
    .join(LocationStarTable, JoinType.LEFT, LocationTable.id, LocationStarTable.locationId,
        additionalConstraint = LocationStarTable.getConstraint(callerId))
    .select(LocationColumns)

val LocationColumns = listOf(
    LocationTable.id,
    LocationTable.cityId,
    LocationTable.mapId,
    LocationTable.timezoneId,
    LocationTable.slug,
    LocationTable.host,
    LocationTable.scout,
    LocationTable.name,
    LocationTable.description,
    LocationTable.address,
    LocationTable.city,
    LocationTable.state,
    LocationTable.geoPoint,
    LocationTable.mapRank,
    LocationTable.mapCategory,
    LocationTable.mapType,
    LocationTable.resources,
    LocationTable.hours,
    LocationTable.website,
    LocationTable.eventsUrl,
    LocationTable.links,
    LocationTable.image,
    LocationTable.starCount,
    LocationTable.updatedAt,
    LocationTable.createdAt,
    LocationStarTable.starId,
)

fun ResultRow.toLocation() = Location(
    locationId = toRecordId(LocationTable.id),
    cityId = this[LocationTable.cityId]?.let { CityId(it.value) },
    mapId = this[LocationTable.mapId],
    timezoneId = this[LocationTable.timezoneId],
    slug = this[LocationTable.slug].toSlug(),
    host = this[LocationTable.host]?.toUsername(),
    scout = this[LocationTable.scout]?.toUsername(),
    name = this[LocationTable.name],
    description = this[LocationTable.description],
    address = this[LocationTable.address],
    city = this[LocationTable.city],
    state = this[LocationTable.state],
    geoPoint = this[LocationTable.geoPoint].toGeoPoint(),
    mapRank = this[LocationTable.mapRank],
    mapCategory = this[LocationTable.mapCategory],
    mapType = this[LocationTable.mapType],
    resources = this[LocationTable.resources].map { ResourceType.entries[it] }.toSet(),
    hours = this[LocationTable.hours],
    website = this[LocationTable.website],
    eventsUrl = this[LocationTable.eventsUrl],
    extraLinks = this[LocationTable.links],
    image = this[LocationTable.image],
    lightCount = this[LocationTable.starCount],
    updatedAt = this[LocationTable.updatedAt],
    createdAt = this[LocationTable.createdAt],
)