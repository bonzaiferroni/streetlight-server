package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import klutch.db.readAll
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import streetlight.model.data.TransitRoute
import streetlight.model.data.TransitRouteId
import streetlight.model.data.TransitStopId
import streetlight.server.db.tables.TransitRouteStopTable
import streetlight.server.db.tables.TransitRouteTable
import streetlight.server.db.tables.toTransitRoute
import streetlight.server.db.tables.createTransitRoute
import streetlight.server.db.tables.updateTransitRoute

/**
 * Ahoy! This be the TransitRouteTableDao, managin' our charts and routes.
 */
class TransitRouteTableDao : DbService() {

    suspend fun readRoute(transitRouteId: TransitRouteId) = dbQuery {
        TransitRouteTable.read { it.id.eq(transitRouteId.value) }.firstOrNull()?.toTransitRoute()
    }

    suspend fun readAllRoutes() = dbQuery {
        TransitRouteTable.readAll().map { it.toTransitRoute() }
    }

    suspend fun readRoutes(routeIds: Set<String>) = dbQuery {
        TransitRouteTable.read { it.id.inList(routeIds) }.map { it.toTransitRoute() }
    }

    suspend fun create(transitRoute: TransitRoute): TransitRouteId = dbQuery {
        TransitRouteTable.insertAndGetId {
            it.createTransitRoute(transitRoute)
        }.value.let { TransitRouteId(it) }
    }

    suspend fun update(transitRoute: TransitRoute) = dbQuery {
        TransitRouteTable.update(where = { TransitRouteTable.id.eq(transitRoute.transitRouteId.value) }) {
            it.updateTransitRoute(transitRoute)
        } == 1
    }

    suspend fun upsert(transitRoute: TransitRoute) = dbQuery {
        TransitRouteTable.upsert(TransitRouteTable.id) {
            it.createTransitRoute(transitRoute)
        }
    }

    suspend fun batchUpsert(transitRoutes: List<TransitRoute>) = dbQuery {
        TransitRouteTable.batchUpsert(transitRoutes, TransitRouteTable.id) {
            this.createTransitRoute(it)
        }
    }

    suspend fun upsertRouteStops(transitRouteId: TransitRouteId, transitStopIds: Iterable<TransitStopId>) = dbQuery {
        TransitRouteStopTable.batchUpsert(transitStopIds) {
            this[TransitRouteStopTable.transitRouteId] = transitRouteId.value
            this[TransitRouteStopTable.transitStopId] = it.value
        }
    }
}
