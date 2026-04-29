package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.EventPost
import streetlight.model.data.LocationPost
import streetlight.model.data.PostType
import streetlight.server.utils.toProjectId

val PostQuery get() = PostTable
    .join(EventTable, JoinType.LEFT, PostTable.eventId, EventTable.id)
    .join(LocationTable, JoinType.LEFT, EventTable.locationId, LocationTable.id)
    .join(StarTable, JoinType.LEFT, PostTable.starId, StarTable.id)
    .select(EventPostColumns)

val EventPostColumns = listOf(
    PostTable.id,
    PostTable.galaxyId,
    PostTable.text,
    PostTable.postType,
    PostTable.createdAt,
    PostTable.updatedAt,
) + EventLocationColumns

val GeneralPostColumns = listOf(
    PostTable.id,
    PostTable.galaxyId,
    PostTable.text,
    PostTable.createdAt,
    PostTable.updatedAt,
    StarTable.username,
) + LocationTable.columns

fun eventJoin() = PostTable
    .join(StarTable, JoinType.LEFT, PostTable.starId, StarTable.id)
    .join(EventTable, JoinType.LEFT, PostTable.eventId, EventTable.id)
    .join(LocationTable, JoinType.LEFT, EventTable.locationId, LocationTable.id)

fun generalJoin() = PostTable
    .join(StarTable, JoinType.LEFT, PostTable.starId, StarTable.id)
    .join(EventTable, JoinType.LEFT, PostTable.eventId, EventTable.id)
    .join(LocationTable, JoinType.LEFT, PostTable.locationId, LocationTable.id)

val PostColumns = (EventPostColumns + GeneralPostColumns).distinct()

fun ResultRow.toPost() = when (this[PostTable.postType]) {
    PostType.Event -> toEventPost()
    PostType.Location -> toLocationPost()
    PostType.Content -> TODO() // toContentPost()
}

fun ResultRow.toEventPost() = EventPost(
    postId = this[PostTable.id].toProjectId(),
    galaxyId = this[PostTable.galaxyId].toProjectId(),
    username = this[StarTable.username],
    event = this.toEventLocation(),
    text = this[PostTable.text],
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt]
)

fun ResultRow.toLocationPost() = LocationPost(
    postId = this[PostTable.id].toProjectId(),
    galaxyId = this[PostTable.galaxyId].toProjectId(),
    username = this[StarTable.username],
    location = this.toLocation(),
    text = this[PostTable.text],
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt]
)