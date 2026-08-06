package streetlight.server.db.services

import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.FetchMode
import streetlight.model.data.SelectorSchema
import streetlight.model.data.LocationId
import streetlight.model.data.Origin
import streetlight.model.data.OriginId
import streetlight.model.data.OriginSchema
import streetlight.model.data.OriginSchemaId
import streetlight.server.db.tables.LocationOriginTable
import streetlight.server.db.tables.OriginSchemaTable
import streetlight.server.db.tables.OriginTable
import streetlight.server.db.tables.createOrigin
import streetlight.server.db.tables.createOriginSchema
import streetlight.server.db.tables.originQuery
import streetlight.server.db.tables.toOrigins
import kotlin.time.Clock

class OriginTableDao : DbService() {

    suspend fun create(originId: OriginId, selectorSchema: SelectorSchema, fetchMode: FetchMode) = dbQuery {
        val schema = selectorSchema.toOriginSchema(originId, fetchMode)
        OriginSchemaTable.insert {
            it.createOriginSchema(schema)
        }
        schema
    }

    suspend fun readOrCreateOrigin(originId: OriginId) = dbQuery {
        readOrigin(originId) ?: originId.toOrigin().also { origin ->
            OriginTable.insert {
                it.createOrigin(origin)
            }
        }
    }

    suspend fun updateRobotsTxt(originId: OriginId, robotsTxt: String) = dbQuery {
        OriginTable.update({ OriginTable.id.eq(originId.value) }) {
            it[OriginTable.robotsTxt] = robotsTxt
            it[OriginTable.updatedAt] = Clock.System.now()
        }
    }

    suspend fun updateSchemaResult(originSchemaId: OriginSchemaId, isSuccess: Boolean) = dbQuery {
        val now = Clock.System.now()
        OriginSchemaTable.update({ OriginSchemaTable.id.eq(originSchemaId) }) {
            it[consecutiveFailCount] = if (isSuccess) intLiteral(0) else consecutiveFailCount + 1
            it[updatedAt] = now
            if (isSuccess) {
                it[lastSuccessAt] = now
            }
        }
    }

    suspend fun linkLocation(originId: OriginId, locationId: LocationId) = dbQuery {
        LocationOriginTable.insertIgnore {
            it[LocationOriginTable.locationId] = locationId.value
            it[LocationOriginTable.originId] = originId.value
        }
    }

    suspend fun readOrigin(originId: OriginId) = dbQuery {
        originQuery().where { OriginTable.id.eq(originId.value) }.toOrigins().firstOrNull()
    }
}

private fun SelectorSchema.toOriginSchema(originId: OriginId, fetchMode: FetchMode) = OriginSchema(
    originSchemaId = OriginSchemaId.random(),
    originId = originId,
    schemaType = schemaType,
    fetchMode = fetchMode,
    selector = this,
    consecutiveFailCount = 0,
    lastSuccessAt = null,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)

private fun OriginId.toOrigin() = Origin(
    originId = this,
    fetchMode = FetchMode.Basic,
    schemas = emptyList(),
    robotsTxt = null,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)