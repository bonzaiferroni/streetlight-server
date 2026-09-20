package streetlight.server.db.services

import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.FetchMode
import streetlight.model.data.OriginId
import streetlight.model.data.Parser
import streetlight.model.data.ParserId
import streetlight.model.data.SelectorSchema
import streetlight.server.db.tables.ParserTable
import streetlight.server.db.tables.createParser
import streetlight.server.db.tables.toParser
import kotlin.time.Clock

class ParserTableDao: DbService() {
    suspend fun create(originId: OriginId, selectorSchema: SelectorSchema, fetchMode: FetchMode) = dbQuery {
        val schema = selectorSchema.toParser(originId, fetchMode)
        ParserTable.insert {
            it.createParser(schema)
        }
        schema
    }

    suspend fun read(originId: OriginId) = dbQuery {
        ParserTable.selectAll().where { ParserTable.originId.eq(originId.value) }.map { it.toParser() }
    }

    suspend fun updateResult(parserId: ParserId, isSuccess: Boolean) = dbQuery {
        val now = Clock.System.now()
        ParserTable.update({ ParserTable.id.eq(parserId) }) {
            it[consecutiveFailCount] = if (isSuccess) intLiteral(0) else consecutiveFailCount + 1
            it[updatedAt] = now
            if (isSuccess) {
                it[lastSuccessAt] = now
            }
        }
    }
}

private fun SelectorSchema.toParser(originId: OriginId, fetchMode: FetchMode) = Parser(
    parserId = ParserId.random(),
    originId = originId,
    schemaType = schemaType,
    fetchMode = fetchMode,
    schema = this,
    consecutiveFailCount = 0,
    lastSuccessAt = null,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)