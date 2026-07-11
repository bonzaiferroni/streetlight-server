package streetlight.server.db.services

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Entity
import streetlight.model.data.StarId
import streetlight.server.db.tables.CityTable
import streetlight.server.db.tables.EventColumns
import streetlight.server.db.tables.EventLocationColumns
import streetlight.server.db.tables.EventStarTable
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationStarTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.PostTable
import streetlight.server.db.tables.getConstraint
import streetlight.server.db.tables.toEventLocation
import streetlight.server.db.tables.toLocation
import kotlin.time.Clock

val CityPostColumns = (EventLocationColumns + EventColumns).distinct()

fun cityPostQuery(starId: StarId?, block: () -> Op<Boolean>) = CityTable
    .leftJoin(LocationTable)
    .join(EventTable, JoinType.LEFT, additionalConstraint = { EventTable.startsAt.greater(Clock.System.now()) })
    .join(LocationStarTable, JoinType.LEFT, PostTable.locationId, LocationStarTable.locationId,
        additionalConstraint = LocationStarTable.getConstraint(starId))
    .join(EventStarTable, JoinType.LEFT, PostTable.eventId, EventStarTable.eventId,
        additionalConstraint = EventStarTable.getConstraint(starId))
    .select(CityPostColumns)
    .where(block)
    .map { it.toCityPost() }

fun ResultRow.toCityPost(): Entity = when (this.getOrNull(EventTable.id)) {
    null -> toLocation()
    else -> toEventLocation()
}