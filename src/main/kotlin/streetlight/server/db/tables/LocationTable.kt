package streetlight.server.db.tables

import kabinet.utils.toInstantFromUtc
import kabinet.utils.toLocalDateTimeUtc
import kampfire.model.UserId
import klutch.db.defaultNow
import klutch.db.tables.UserTable
import klutch.utils.*
import klutch.db.point
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.Location
import streetlight.model.data.ResourceType
import streetlight.server.utils.toProjectId

object LocationTable : UUIDTable("location") {
    val hostId = reference("host_id", UserTable, ReferenceOption.SET_NULL).nullable()
    val name = text("name")
    val description = text("description").nullable()
    val address = text("address").nullable()
    val geoPoint = point("geo_point")
    val resources = array<Int>("resources")
    val link = text("link").nullable()
    val eventsUrl = text("events_url").nullable()
    val aboutUrl = text("about_url").nullable()
    val menuUrl = text("menu_url").nullable()
    val imageUrl = text("image_url").nullable()
    val thumbUrl = text("thumb_url").nullable()
    val updatedAt = datetime("updated_at").defaultNow()
    val createdAt = datetime("created_at").defaultNow()
}

fun ResultRow.toLocation() = Location(
    locationId = toProjectId(LocationTable.id),
    name = this[LocationTable.name],
    description = this[LocationTable.description],
    address = this[LocationTable.address],
    geoPoint = this[LocationTable.geoPoint].toGeoPoint(),
    resources = this[LocationTable.resources].map { ResourceType.entries[it] }.toSet(),
    website = this[LocationTable.link],
    eventsUrl = this[LocationTable.eventsUrl],
    aboutUrl = this[LocationTable.aboutUrl],
    menuUrl = this[LocationTable.menuUrl],
    imageUrl = this[LocationTable.imageUrl],
    thumbUrl = this[LocationTable.thumbUrl],
    updatedAt = this[LocationTable.updatedAt].toInstantFromUtc(),
    createdAt = this[LocationTable.createdAt].toInstantFromUtc(),
)

// Updaters
fun UpdateBuilder<*>.writeFull(location: Location, userId: UserId?) {
    this[LocationTable.id] = location.locationId.toUUID()
    this[LocationTable.hostId] = userId?.toUUID()
    this[LocationTable.createdAt] = location.createdAt.toLocalDateTimeUtc()
    writeUpdate(location)
}

fun UpdateBuilder<*>.writeUpdate(location: Location) {
    this[LocationTable.name] = location.name
    this[LocationTable.description] = location.description
    this[LocationTable.address] = location.address
    this[LocationTable.geoPoint] = location.geoPoint.toPGpoint()
    this[LocationTable.resources] = location.resources.map { it.ordinal }
    this[LocationTable.link] = location.website
    this[LocationTable.eventsUrl] = location.eventsUrl
    this[LocationTable.aboutUrl] = location.aboutUrl
    this[LocationTable.menuUrl] = location.menuUrl
    this[LocationTable.imageUrl] = location.imageUrl
    this[LocationTable.thumbUrl] = location.thumbUrl
    this[LocationTable.updatedAt] = location.updatedAt.toLocalDateTimeUtc()
}