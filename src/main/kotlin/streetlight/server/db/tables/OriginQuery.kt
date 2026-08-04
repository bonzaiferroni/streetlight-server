package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Origin
import streetlight.model.data.OriginId
import streetlight.model.data.OriginSchema
import streetlight.server.utils.toRecordId

object OriginQuery {
    val columns = OriginTable.columns + OriginSchemaTable.columns
}

fun originQuery() = OriginTable.leftJoin(OriginSchemaTable).select(OriginQuery.columns)

fun Query.toOrigins() = groupBy { it[OriginTable.id].value }
    .map { (_, rows) ->
        rows.first().toOrigin(rows.map { it.toOriginSchema() })
    }

fun ResultRow.toOriginSchema() = OriginSchema(
    originSchemaId = toRecordId(OriginSchemaTable.id),
    originId = OriginId(this[OriginSchemaTable.originId].value),
    schemaType = this[OriginSchemaTable.schemaType],
    content = this[OriginSchemaTable.content],
    consecutiveFailCount = this[OriginSchemaTable.consecutiveFailCount],
    lastSuccessAt = this[OriginSchemaTable.lastSuccessAt],
    updatedAt = this[OriginSchemaTable.updatedAt],
    createdAt = this[OriginSchemaTable.createdAt],
)

fun ResultRow.toOrigin(schemas: List<OriginSchema>) = Origin(
    originId = OriginId(this[OriginTable.id].value),
    schemas = schemas,
    robotsTxt = this[OriginTable.robotsTxt],
    updatedAt = this[OriginTable.updatedAt],
    createdAt = this[OriginTable.createdAt]
)