package streetlight.server.db.tables

import kampfire.api.toSlug
import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.BasicPost
import streetlight.model.data.EventPost
import streetlight.model.data.LocationPost
import streetlight.model.data.PostType
import streetlight.model.data.StarId
import streetlight.server.utils.toRecordId

val PostColumns = listOf(
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
    PostTable.postType,
    PostTable.createdAt,
    PostTable.updatedAt,
    PostLightTable.starId
)

val PostQueryColumns = (EventLocationColumns + LocationColumns + PostColumns).distinct()

fun postQuery(starId: StarId?) = PostTable
    .join(EventTable, JoinType.LEFT, PostTable.eventId, EventTable.id)
    .join(LocationTable, JoinType.LEFT, PostTable.locationId, LocationTable.id)
    .join(PostLightTable, JoinType.LEFT, PostTable.id, PostLightTable.postId,
        additionalConstraint = PostLightTable.getConstraint(starId))
    .join(EventLightTable, JoinType.LEFT, PostTable.eventId, EventLightTable.starId,
        additionalConstraint = EventLightTable.getConstraint(starId))
    .join(LocationLightTable, JoinType.LEFT, PostTable.locationId, LocationLightTable.locationId,
        additionalConstraint = LocationLightTable.getConstraint(starId))
    .select(PostQueryColumns)

fun ResultRow.toPost() = when (this[PostTable.postType]) {
    PostType.Event -> toEventPost()
    PostType.Location -> toLocationPost()
    PostType.Content -> toBasicPost()
}

fun ResultRow.toEventPost() = EventPost(
    postId = this[PostTable.id].toRecordId(),
    slug = this[PostTable.slug].toSlug(),
    galaxyId = this[PostTable.galaxyId].toRecordId(),
    username = this[PostTable.username],
    event = this.toEventLocation(),
    text = this[PostTable.text],
    isLit = this.getOrNull(PostLightTable.starId) != null,
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
    isLit = this.getOrNull(PostLightTable.starId) != null,
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt]
)

fun ResultRow.toBasicPost() = BasicPost(
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
    isLit = this.getOrNull(PostLightTable.starId) != null,
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt]
)