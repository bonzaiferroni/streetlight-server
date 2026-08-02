package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ResultRow
import streetlight.model.data.LocationConfig
import streetlight.server.utils.toRecordId

object LocationConfigQuery {
    val columns = LocationQuery.columns + listOf(
        LocationTable.eventSchema
    )
}

fun ResultRow.toLocationConfig() = LocationConfig(
    location = toLocation(),
    eventSchema = this[LocationTable.eventSchema],
)