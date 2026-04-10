package streetlight.server.db.tables

import kampfire.model.ImageSize
import klutch.utils.*
import klutch.db.point
import klutch.db.scaledImages
import klutch.db.url
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Location
import streetlight.model.data.ResourceType
import streetlight.model.data.StarId
import streetlight.server.utils.toProjectId

object LocationTable : UUIDTable("location") {
    val creatorId = reference("creator_id", StarTable, ReferenceOption.SET_NULL).nullable()
    val ownerId = reference("owner_id", StarTable, ReferenceOption.SET_NULL).nullable()
    val name = text("name")
    val description = text("description").nullable()
    val address = text("address").nullable()
    val city = text("city").nullable()
    val geoPoint = point("geo_point")
    val resources = array<Int>("resources")
    val website = text("link").nullable()
    val eventsUrl = text("events_url").nullable()
    val aboutUrl = text("about_url").nullable()
    val menuUrl = text("menu_url").nullable()
    val imageRef = url("image_ref").nullable()
    val images = scaledImages("images").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    val imageConfig = imageConfigOf(
        table = this,
        refColumn = imageRef,
        arrayColumn = images,
        ImageSize.Large,
        ImageSize.Medium,
        ImageSize.Small,
        ImageSize.Thumb,
    )
}

fun ResultRow.toLocation() = Location(
    locationId = toProjectId(LocationTable.id),
    name = this[LocationTable.name],
    description = this[LocationTable.description],
    address = this[LocationTable.address],
    city = this[LocationTable.city],
    geoPoint = this[LocationTable.geoPoint].toGeoPoint(),
    resources = this[LocationTable.resources].map { ResourceType.entries[it] }.toSet(),
    website = this[LocationTable.website],
    eventsUrl = this[LocationTable.eventsUrl],
    aboutUrl = this[LocationTable.aboutUrl],
    menuUrl = this[LocationTable.menuUrl],
    imageRef = this[LocationTable.imageRef],
    images = this[LocationTable.images],
    updatedAt = this[LocationTable.updatedAt],
    createdAt = this[LocationTable.createdAt],
)

// Updaters
fun UpdateBuilder<*>.writeFull(location: Location, creatorId: StarId?, imageSet: SavedImageSet?) {
    this[LocationTable.id] = location.locationId.toUUID()
    this[LocationTable.creatorId] = creatorId?.toUUID()
    this[LocationTable.createdAt] = location.createdAt
    writeUpdate(location, imageSet)
}

fun UpdateBuilder<*>.writeUpdate(location: Location, imageSet: SavedImageSet?) {
    // this[LocationTable.ownerId] = ownerId?.toUUID() // td: set owner identity with special pipeline
    this[LocationTable.name] = location.name
    this[LocationTable.description] = location.description
    this[LocationTable.address] = location.address
    this[LocationTable.city] = location.city
    this[LocationTable.geoPoint] = location.geoPoint.toPGpoint()
    this[LocationTable.resources] = location.resources.map { it.ordinal }
    this[LocationTable.website] = location.website
    this[LocationTable.eventsUrl] = location.eventsUrl
    this[LocationTable.aboutUrl] = location.aboutUrl
    this[LocationTable.menuUrl] = location.menuUrl
    this[LocationTable.updatedAt] = location.updatedAt
    writeImages(LocationTable.imageConfig, imageSet)
}