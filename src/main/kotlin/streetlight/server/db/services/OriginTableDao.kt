package streetlight.server.db.services

import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.FetchMode
import streetlight.model.data.SelectorSchema
import streetlight.model.data.LocationId
import streetlight.model.data.Origin
import streetlight.model.data.OriginId
import streetlight.model.data.Parser
import streetlight.model.data.ParserId
import streetlight.server.db.tables.LocationOriginTable
import streetlight.server.db.tables.ParserTable
import streetlight.server.db.tables.OriginTable
import streetlight.server.db.tables.createOrigin
import streetlight.server.db.tables.createOriginSchema
import streetlight.server.db.tables.toOrigin
import kotlin.time.Clock

class OriginTableDao : DbService() {

    suspend fun readOrCreateOrigin(originId: OriginId) = dbQuery {
        readOrigin(originId) ?: originId.toOrigin().also { origin ->
            OriginTable.insert {
                it.createOrigin(origin)
            }
        }
    }

    suspend fun registerIncomplete(originId: OriginId) = dbQuery {
        OriginTable.update({ OriginTable.id.eq(originId.value)}) {
            it[OriginTable.fetchMode] = FetchMode.Scripting
        }
    }

    suspend fun updateRobotsTxt(originId: OriginId, robotsTxt: String) = dbQuery {
        OriginTable.update({ OriginTable.id.eq(originId.value) }) {
            it[OriginTable.robotsTxt] = robotsTxt
            it[OriginTable.updatedAt] = Clock.System.now()
        }
    }

    suspend fun linkLocation(originId: OriginId, locationId: LocationId) = dbQuery {
        LocationOriginTable.insertIgnore {
            it[LocationOriginTable.locationId] = locationId.value
            it[LocationOriginTable.originId] = originId.value
        }
    }

    suspend fun readOrigin(originId: OriginId) = dbQuery {
        OriginTable.selectAll().where { OriginTable.id.eq(originId.value) }.map { it.toOrigin() }.singleOrNull()
    }

    suspend fun readFetchMode(originId: OriginId) = dbQuery {
        OriginTable.select(OriginTable.fetchMode).where { OriginTable.id.eq(originId.value) }
            .map { it[OriginTable.fetchMode] }.singleOrNull()
    }
}

private fun OriginId.toOrigin() = Origin(
    originId = this,
    fetchMode = FetchMode.Basic,
    robotsTxt = null,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)