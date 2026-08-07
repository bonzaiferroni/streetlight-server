package streetlight.server.db.tables

import klutch.db.jsonColumnConfig
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.FetchMode
import streetlight.model.data.SelectorSchema
import streetlight.model.data.Parser
import streetlight.model.data.SchemaType

object ParserTable: UuidTable("parser") {
    val originId = reference("origin_id", OriginTable, ReferenceOption.CASCADE).index()
    val schemaType = enumeration<SchemaType>("schema_type")
    val fetchMode = enumeration<FetchMode>("fetch_mode").default(FetchMode.Basic)
    val schema = jsonb<SelectorSchema>("schema", jsonColumnConfig)
    val consecutiveFailCount = integer("consecutive_fail_count").default(0)
    val lastSuccessAt = timestamp("last_success_at").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun UpdateBuilder<*>.createOriginSchema(schema: Parser) {
    this[ParserTable.originId] = schema.originId.value
    this[ParserTable.createdAt] = schema.createdAt
    updateOriginSchema(schema)
}

fun UpdateBuilder<*>.updateOriginSchema(schema: Parser) {
    this[ParserTable.schemaType] = schema.schemaType
    this[ParserTable.fetchMode] = schema.fetchMode
    this[ParserTable.schema] = schema.schema
    this[ParserTable.consecutiveFailCount] = schema.consecutiveFailCount
    this[ParserTable.lastSuccessAt] = schema.lastSuccessAt
    this[ParserTable.updatedAt] = schema.updatedAt
}