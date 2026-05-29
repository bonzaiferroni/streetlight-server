package streetlight.server.db.tables

import kampfire.api.toSlug
import kampfire.model.thumb
import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Post
import streetlight.model.data.EventPost
import streetlight.model.data.LocationPost
import streetlight.model.data.PostType
import streetlight.server.utils.toRecordId

val PostQuery get() = PostTable
    .join(EventTable, JoinType.LEFT, PostTable.eventId, EventTable.id)
    .join(LocationTable, JoinType.LEFT, EventTable.locationId, LocationTable.id)
    .join(StarTable, JoinType.LEFT, PostTable.starId, StarTable.id)
    .select(EventPostColumns)

val EventPostColumns = listOf(
    PostTable.id,
    PostTable.galaxyId,
    PostTable.slug,
    PostTable.text,
    PostTable.postType,
    PostTable.createdAt,
    PostTable.updatedAt,
) + EventLocationColumns

val GeneralPostColumns = listOf(
    PostTable.id,
    PostTable.galaxyId,
    PostTable.slug,
    PostTable.username,
    PostTable.title,
    PostTable.subtitle,
    PostTable.text,
    PostTable.geoPoint,
    PostTable.imageRef,
    PostTable.images,
    PostTable.links,
    PostTable.createdAt,
    PostTable.updatedAt,
) + LocationTable.columns

fun eventJoin() = PostTable
    .join(EventTable, JoinType.LEFT, PostTable.eventId, EventTable.id)
    .join(LocationTable, JoinType.LEFT, EventTable.locationId, LocationTable.id)

fun generalJoin() = PostTable
    .join(EventTable, JoinType.LEFT, PostTable.eventId, EventTable.id)
    .join(LocationTable, JoinType.LEFT, PostTable.locationId, LocationTable.id)

val PostColumns = (EventPostColumns + GeneralPostColumns).distinct()

fun ResultRow.toPost() = when (this[PostTable.postType]) {
    PostType.Event -> toEventPost()
    PostType.Location -> toLocationPost()
    PostType.Content -> toContentPost()
}

fun ResultRow.toEventPost() = EventPost(
    postId = this[PostTable.id].toRecordId(),
    slug = this[PostTable.slug].toSlug(),
    galaxyId = this[PostTable.galaxyId].toRecordId(),
    username = this[PostTable.username],
    event = this.toEventLocation(),
    text = this[PostTable.text],
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt]
)

fun ResultRow.toLocationPost() = LocationPost(
    postId = this[PostTable.id].toRecordId(),
    slug = this[PostTable.slug].toSlug(),
    galaxyId = this[PostTable.galaxyId].toRecordId(),
    username = this[PostTable.username],
    location = this.toLocation(),
    text = this[PostTable.text],
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt]
)

fun ResultRow.toContentPost() = Post(
    postId = this[PostTable.id].toRecordId(),
    slug = this[PostTable.slug].toSlug(),
    galaxyId = this[PostTable.galaxyId].toRecordId(),
    username = this[PostTable.username],
    title = this[PostTable.title] ?: error("Title not found"),
    subtitle = this[PostTable.subtitle],
    text = this[PostTable.text],
    geoPoint = this[PostTable.geoPoint]?.toGeoPoint(),
    links = this[PostTable.links],
    imageRef = this[PostTable.imageRef],
    images = this[PostTable.images],
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt]
)