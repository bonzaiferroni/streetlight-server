package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ResultRow
import streetlight.model.data.LocationConfig
import streetlight.model.data.LocationConfigContent
import streetlight.server.utils.toRecordId

object LocationConfigQuery {
    val columns = listOf(
        LocationTable.id,
        LocationTable.eventSchema
    )

    val parseColumns = listOf(
        LocationTable.parseResult,
        LocationTable.parsedAt,
    )

    val contentColumns = (columns + parseColumns + LocationQuery.columns).distinct()

}

fun ResultRow.toLocationConfigContent() = LocationConfigContent(
    location = toLocation(),
    config = toLocationConfig(),
    parseResult = this[LocationTable.parseResult],
    parsedAt = this[LocationTable.parsedAt],
)

fun ResultRow.toLocationConfig() = LocationConfig(
    locationId = toRecordId(LocationTable.id),
    eventSchema = this[LocationTable.eventSchema],
)