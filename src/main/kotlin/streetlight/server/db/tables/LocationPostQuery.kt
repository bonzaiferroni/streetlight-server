package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.LocationPost
import streetlight.server.utils.toProjectId

val LocationPostQuery get() = PostTable
    .join(LocationTable, JoinType.LEFT, PostTable.locationId, LocationTable.id)
    .join(StarTable, JoinType.LEFT, PostTable.starId, StarTable.id)
    .select(LocationPostColumns)

val LocationPostColumns = listOf(
    PostTable.id,
    PostTable.galaxyId,
    PostTable.text,
    PostTable.createdAt,
    PostTable.updatedAt,
    StarTable.username,
) + LocationTable.columns

fun ResultRow.toLocationPost() = LocationPost(
    postId = this[PostTable.id].toProjectId(),
    galaxyId = this[PostTable.galaxyId].toProjectId(),
    username = this[StarTable.username],
    location = this.toLocation(),
    text = this[PostTable.text],
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt]
)