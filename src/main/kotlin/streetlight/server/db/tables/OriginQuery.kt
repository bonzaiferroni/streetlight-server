package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ResultRow
import streetlight.model.data.Origin
import streetlight.model.data.OriginId
import streetlight.model.data.Parser
import streetlight.server.utils.toRecordId

object OriginQuery {

}

fun ResultRow.toParser() = Parser(
    parserId = toRecordId(ParserTable.id),
    originId = OriginId(this[ParserTable.originId].value),
    schemaType = this[ParserTable.schemaType],
    fetchMode = this[ParserTable.fetchMode],
    schema = this[ParserTable.schema],
    consecutiveFailCount = this[ParserTable.consecutiveFailCount],
    lastSuccessAt = this[ParserTable.lastSuccessAt],
    updatedAt = this[ParserTable.updatedAt],
    createdAt = this[ParserTable.createdAt],
)

fun ResultRow.toOrigin() = Origin(
    originId = OriginId(this[OriginTable.id].value),
    fetchMode = this[OriginTable.fetchMode],
    robotsTxt = this[OriginTable.robotsTxt],
    updatedAt = this[OriginTable.updatedAt],
    createdAt = this[OriginTable.createdAt]
)