package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.LocationConfig
import streetlight.model.data.LocationConfigContent
import streetlight.model.data.Origin
import streetlight.server.utils.toRecordId

object LocationConfigQuery {
    val columns = listOf(
        LocationTable.id,
    )

    val contentColumns = (columns + LocationQuery.columns + OriginQuery.columns).distinct()
}

fun locationConfigContentQuery() = LocationTable
    .leftJoin(LocationOriginTable)
    .leftJoin(OriginTable)
    .leftJoin(OriginSchemaTable)
    .select(LocationConfigQuery.contentColumns)

fun Query.toLocationConfigContent() = groupBy { it[LocationTable.id].value }
    .map { (_, originRows) ->
        val origins = originRows.groupBy { it.getOrNull(OriginTable.id)?.value }
            .mapNotNull { (originId, schemaRows) ->
                if (originId == null) return@mapNotNull null
                val schemas = schemaRows.mapNotNull { row ->
                    row.takeIf { it.getOrNull(OriginSchemaTable.id) != null }?.toOriginSchema()
                }
                schemaRows.first().toOrigin(schemas)
            }
        originRows.first().toLocationConfigContent(origins)
    }

fun ResultRow.toLocationConfigContent(origins: List<Origin>) = LocationConfigContent(
    location = toLocation(),
    config = toLocationConfig(),
    origins = origins,
)

fun ResultRow.toLocationConfig() = LocationConfig(
    locationId = toRecordId(LocationTable.id),
)

