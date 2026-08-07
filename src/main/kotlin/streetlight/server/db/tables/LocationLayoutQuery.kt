package streetlight.server.db.tables

import klutch.db.model.CallerId
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.LocationLayout

object LocationLayoutQuery {
    val columns = listOf(LocationTable.layout) + LocationQuery.columns
}

fun locationLayoutQuery(callerId: CallerId?) = LocationTable
    .join(LocationStarTable, JoinType.LEFT, LocationTable.id, LocationStarTable.locationId,
        additionalConstraint = LocationStarTable.getConstraint(callerId))
    .select(LocationLayoutQuery.columns)

fun ResultRow.toLocationLayout() = LocationLayout(
    location = toLocation(),
    layout = this[LocationTable.layout]
)