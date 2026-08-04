package streetlight.server.db.services

import kampfire.model.Url
import klutch.db.DbService
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.ContentSchema
import streetlight.model.data.LocationId
import streetlight.model.data.Origin
import streetlight.model.data.OriginId
import streetlight.model.data.OriginSchema
import streetlight.model.data.OriginSchemaId
import streetlight.model.data.toOriginId
import streetlight.server.db.tables.LocationOriginTable
import streetlight.server.db.tables.OriginSchemaTable
import streetlight.server.db.tables.OriginTable
import streetlight.server.db.tables.createOrigin
import streetlight.server.db.tables.createOriginSchema
import streetlight.server.db.tables.toOrigin
import kotlin.time.Clock

class OriginTableDao: DbService() {

    suspend fun create(originId: OriginId, schema: ContentSchema) = dbQuery {
        OriginSchemaTable.insert {
            it.createOriginSchema(schema.toOriginSchema(originId))
        }
    }

    suspend fun readOrCreateOrigin(originId: OriginId) = dbQuery {
        OriginTable.select(OriginTable.columns).where { OriginTable.id.eq(originId.value) }.singleOrNull()?.toOrigin(emptyList())
            ?: originId.toOrigin().also { origin ->
                OriginTable.insert {
                    it.createOrigin(origin)
                }
            }
    }

    suspend fun linkLocation(originId: OriginId, locationId: LocationId) = dbQuery {
        LocationOriginTable.insertIgnore {
            it[LocationOriginTable.locationId] = locationId.value
            it[LocationOriginTable.originId] = originId.value
        }
    }
}

private fun ContentSchema.toOriginSchema(originId: OriginId) = OriginSchema(
    originSchemaId = OriginSchemaId.random(),
    originId = originId,
    schemaType = schemaType,
    content = this,
    consecutiveFailCount = 0,
    lastSuccessAt = null,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)

private fun OriginId.toOrigin() = Origin(
    originId = this,
    schemas = emptyList(),
    robotsTxt = null,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)