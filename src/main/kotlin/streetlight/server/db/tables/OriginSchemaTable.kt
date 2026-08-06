package streetlight.server.db.tables

import klutch.db.jsonColumnConfig
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.FetchMode
import streetlight.model.data.SelectorSchema
import streetlight.model.data.OriginSchema
import streetlight.model.data.SchemaType

object OriginSchemaTable: UuidTable("origin_schema") {
    val originId = reference("origin_id", OriginTable, ReferenceOption.CASCADE).index()
    val schemaType = enumeration<SchemaType>("schema_type")
    val fetchMode = enumeration<FetchMode>("fetch_mode").default(FetchMode.Basic)
    val content = jsonb<SelectorSchema>("content", jsonColumnConfig)
    val consecutiveFailCount = integer("consecutive_fail_count").default(0)
    val lastSuccessAt = timestamp("last_success_at").nullable()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun UpdateBuilder<*>.createOriginSchema(schema: OriginSchema) {
    this[OriginSchemaTable.originId] = schema.originId.value
    this[OriginSchemaTable.createdAt] = schema.createdAt
    updateOriginSchema(schema)
}

fun UpdateBuilder<*>.updateOriginSchema(schema: OriginSchema) {
    this[OriginSchemaTable.schemaType] = schema.schemaType
    this[OriginSchemaTable.fetchMode] = schema.fetchMode
    this[OriginSchemaTable.content] = schema.selector
    this[OriginSchemaTable.consecutiveFailCount] = schema.consecutiveFailCount
    this[OriginSchemaTable.lastSuccessAt] = schema.lastSuccessAt
    this[OriginSchemaTable.updatedAt] = schema.updatedAt
}