package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.EventPost
import streetlight.server.utils.toProjectId

val EventPostQuery get() = EventPostTable.join(EventTable, JoinType.LEFT, EventPostTable.eventId, EventTable.id)
    .join(LocationTable, JoinType.LEFT, EventTable.locationId, LocationTable.id)
    .join(StarTable, JoinType.LEFT, EventTable.starId, StarTable.id)
    .select(EventPostColumns)

val EventPostColumns = listOf(
    EventPostTable.id,
    EventPostTable.galaxyId,
    EventPostTable.text,
    EventPostTable.createdAt,
    EventPostTable.updatedAt,
) + EventLocationColumns

fun ResultRow.toEventPost() = EventPost(
    postId = this[EventPostTable.id].toProjectId(),
    galaxyId = this[EventPostTable.galaxyId].toProjectId(),
    event = this.toEventLocation(),
    text = this[EventPostTable.text],
    createdAt = this[EventPostTable.createdAt],
    updatedAt = this[EventPostTable.updatedAt]
)