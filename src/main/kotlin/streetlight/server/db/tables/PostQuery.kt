package streetlight.server.db.tables

import kampfire.api.toSlug
import kampfire.api.toUsername
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.MediumPost
import streetlight.model.data.EventPost
import streetlight.model.data.LocationPost
import streetlight.model.data.Post
import streetlight.model.data.PostType
import streetlight.model.data.StarId
import streetlight.server.utils.toRecordId

val PostColumns = listOf(
    PostTable.id,
    PostTable.galaxyId,
    PostTable.mediumId,
    PostTable.galaxySlug,
    PostTable.galaxyName,
    PostTable.postType,
    PostTable.username,
    PostTable.title,
    PostTable.text,
    PostTable.starCount,
    PostTable.createdAt,
    PostTable.updatedAt,
    PostStarTable.starId
)

val PostQueryColumns = (EventLocationColumns + LocationColumns + PostColumns + MediumColumns).distinct()

fun postQuery(starId: StarId?) = PostTable
    .leftJoin(EventTable)
    .join(LocationTable, JoinType.LEFT, PostTable.locationId, LocationTable.id)
    .leftJoin(MediumTable)
    .join(PostStarTable, JoinType.LEFT, PostTable.id, PostStarTable.postId,
        additionalConstraint = PostStarTable.getConstraint(starId))
    .join(EventStarTable, JoinType.LEFT, PostTable.eventId, EventStarTable.eventId,
        additionalConstraint = EventStarTable.getConstraint(starId))
    .join(LocationStarTable, JoinType.LEFT, PostTable.locationId, LocationStarTable.locationId,
        additionalConstraint = LocationStarTable.getConstraint(starId))
    .select(PostQueryColumns)

fun ResultRow.toGalaxyPost() = when (this[PostTable.postType]) {
    PostType.Event -> EventPost(
        event = this.toEventLocation(),
        base = this.toPost(),
    )
    PostType.Location -> LocationPost(
        base = this.toPost(),
        location = this.toLocation(),
    )
    PostType.Medium -> MediumPost(
        medium = this.toMedium(),
        base = this.toPost()
    )
}

fun ResultRow.toPost() = Post(
    postId = this[PostTable.id].toRecordId(),
    galaxyId = this[PostTable.galaxyId].toRecordId(),
    postType = this[PostTable.postType],
    galaxyName = this[PostTable.galaxyName] ?: error("galaxy name not found"),
    galaxySlug = this[PostTable.galaxySlug]?.toSlug() ?: error("galaxy slug not found"),
    username = this[PostTable.username]?.toUsername(),
    title = this[PostTable.title],
    text = this[PostTable.text],
    isLit = this.getOrNull(PostStarTable.starId) != null,
    lightCount = this[PostTable.starCount],
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt],
)
