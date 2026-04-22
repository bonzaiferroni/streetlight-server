package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.EventPost
import streetlight.model.data.LocationPost
import streetlight.server.utils.toProjectId

val LocationPostQuery get() = LocationPostTable
    .join(LocationTable, JoinType.LEFT, LocationPostTable.locationId, LocationTable.id)
    .join(StarTable, JoinType.LEFT, LocationPostTable.starId, StarTable.id)
    .join(LocationLightTable, JoinType.LEFT, LocationPostTable.locationId, LocationLightTable.locationId)
    .select(LocationPostColumns)
    .groupBy(LocationPostTable.id, LocationTable.id, StarTable.id)

val LocationPostColumns = listOf(
    LocationPostTable.id,
    LocationPostTable.galaxyId,
    LocationPostTable.text,
    LocationPostTable.createdAt,
    LocationPostTable.updatedAt,
    StarTable.username,
) + LocationTable.columns

fun ResultRow.toLocationPost() = LocationPost(
    postId = this[LocationPostTable.id].toProjectId(),
    galaxyId = this[LocationPostTable.galaxyId].toProjectId(),
    username = this[StarTable.username],
    location = this.toLocation(),
    text = this[LocationPostTable.text],
    createdAt = this[LocationPostTable.createdAt],
    updatedAt = this[LocationPostTable.updatedAt]
)