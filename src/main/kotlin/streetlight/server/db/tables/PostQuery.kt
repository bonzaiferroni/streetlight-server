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
    PostTable.galaxySlug,
    PostTable.galaxyName,
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
    PostTable.starCount,
    PostTable.createdAt,
    PostTable.updatedAt,
    PostStarTable.starId
)

val PostQueryColumns = (EventLocationColumns + LocationColumns + PostColumns).distinct()

fun postQuery(starId: StarId?) = PostTable
    .join(EventTable, JoinType.LEFT, PostTable.eventId, EventTable.id)
    .join(LocationTable, JoinType.LEFT, PostTable.locationId, LocationTable.id)
    .join(PostStarTable, JoinType.LEFT, PostTable.id, PostStarTable.postId,
        additionalConstraint = PostStarTable.getConstraint(starId))
    .join(EventStarTable, JoinType.LEFT, PostTable.eventId, EventStarTable.eventId,
        additionalConstraint = EventStarTable.getConstraint(starId))
    .join(LocationStarTable, JoinType.LEFT, PostTable.locationId, LocationStarTable.locationId,
        additionalConstraint = LocationStarTable.getConstraint(starId))
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
    galaxyName = this[PostTable.galaxyName] ?: error("galaxy name not found"),
    galaxySlug = this[PostTable.galaxySlug]?.toSlug() ?: error("galaxy slug not found"),
    username = this[PostTable.username],
    event = this.toEventLocation(),
    text = this[PostTable.text],
    isLit = this.getOrNull(PostStarTable.starId) != null,
    lightCount = this[PostTable.starCount],
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt]
)

fun ResultRow.toLocationPost() = LocationPost(
    postId = this[PostTable.id].toRecordId(),
    slug = this[PostTable.slug].toSlug(),
    galaxyId = this[PostTable.galaxyId].toRecordId(),
    galaxyName = this[PostTable.galaxyName] ?: error("galaxy name not found"),
    galaxySlug = this[PostTable.galaxySlug]?.toSlug() ?: error("galaxy slug not found"),
    username = this[PostTable.username],
    location = this.toLocation(),
    text = this[PostTable.text],
    isLit = this.getOrNull(PostStarTable.starId) != null,
    lightCount = this[PostTable.starCount],
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt]
)

fun ResultRow.toBasicPost() = BasicPost(
    postId = this[PostTable.id].toRecordId(),
    slug = this[PostTable.slug].toSlug(),
    galaxyId = this[PostTable.galaxyId].toRecordId(),
    galaxyName = this[PostTable.galaxyName] ?: error("galaxy name not found"),
    galaxySlug = this[PostTable.galaxySlug]?.toSlug() ?: error("galaxy slug not found"),
    username = this[PostTable.username],
    label = this[PostTable.title] ?: error("Title not found"),
    sublabel = this[PostTable.subtitle],
    text = this[PostTable.text],
    geoPoint = this[PostTable.geoPoint]?.toGeoPoint(),
    links = this[PostTable.links],
    imageRef = this[PostTable.imageRef],
    images = this[PostTable.images],
    isLit = this.getOrNull(PostStarTable.starId) != null,
    lightCount = this[PostTable.starCount],
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt]
)