package streetlight.server.db.tables

import kampfire.api.toSlug
import kampfire.api.toUsername
import klutch.db.model.CallerId
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.wrapAsExpression
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.MediaPost
import streetlight.model.data.EventPost
import streetlight.model.data.GalaxyTrace
import streetlight.model.data.Lean
import streetlight.model.data.LocationPost
import streetlight.model.data.Post
import streetlight.model.data.PostType
import streetlight.server.utils.toRecordId

object GalaxyPostAspect {
    val PostColumns = listOf(
        PostTable.id,
        PostTable.galaxyId,
        PostTable.mediaId,
        PostTable.galaxySlug,
        PostTable.galaxyName,
        PostTable.postType,
        PostTable.username,
        PostTable.title,
        PostTable.text,
        PostTable.starCount,
        PostTable.createdAt,
        PostTable.updatedAt,
    )

    val GalaxyPostColumns = (EventLocationColumns + LocationQuery.columns + PostColumns + MediaColumns).distinct()

    fun query(callerId: CallerId?, userLean: Expression<Lean?>) = PostTable
        .leftJoin(EventTable)
        .join(LocationTable, JoinType.LEFT, PostTable.locationId, LocationTable.id)
        .leftJoin(MediaTable)
        .join(EventStarTable, JoinType.LEFT, PostTable.eventId, EventStarTable.eventId,
            additionalConstraint = EventStarTable.getConstraint(callerId))
        .join(LocationStarTable, JoinType.LEFT, PostTable.locationId, LocationStarTable.locationId,
            additionalConstraint = LocationStarTable.getConstraint(callerId))
        .select(GalaxyPostColumns + userLean)

    fun userLean(callerId: CallerId?): Expression<Lean?> = callerId?.let {
        wrapAsExpression(
            PostMarkTable
                .join(GalaxyMarkTable, JoinType.INNER, PostMarkTable.markId, GalaxyMarkTable.markId,
                    additionalConstraint = { GalaxyMarkTable.galaxyId.eq(PostTable.galaxyId) })
                .select(GalaxyMarkTable.lean)
                .where { PostMarkTable.postId.eq(PostTable.id) and PostMarkTable.starId.eq(it) }
                .limit(1)
        )
    } ?: Op.nullOp()
}

fun ResultRow.toGalaxyPost(userLean: Expression<Lean?>? = null) = when (this[PostTable.postType]) {
    PostType.Event -> EventPost(
        event = this.toEventLocation(),
        base = this.toPost(userLean),
    )
    PostType.Location -> LocationPost(
        base = this.toPost(userLean),
        location = this.toLocation(),
    )
    PostType.Media -> MediaPost(
        media = this.toMedia(),
        base = this.toPost(userLean)
    )
}

fun ResultRow.toPost(userLean: Expression<Lean?>? = null) = Post(
    postId = this[PostTable.id].toRecordId(),
    galaxy = toGalaxyTrace(),
    postType = this[PostTable.postType],
    username = this[PostTable.username]?.toUsername(),
    title = this[PostTable.title],
    text = this[PostTable.text],
    lean = userLean?.let { this[it] },
    lightCount = this[PostTable.starCount],
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt],
)

fun ResultRow.toGalaxyTrace() = GalaxyTrace(
    galaxyId = this[PostTable.galaxyId].toRecordId(),
    name = this[PostTable.galaxyName] ?: error("galaxy name not found"),
    slug = this[PostTable.galaxySlug]?.toSlug() ?: error("galaxy slug not found"),
)