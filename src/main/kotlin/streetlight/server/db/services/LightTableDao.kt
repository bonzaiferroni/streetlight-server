package streetlight.server.db.services

import kotlin.time.Instant
import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.EventId
import streetlight.model.data.GalaxyId
import streetlight.model.data.LightEdit
import streetlight.model.data.LightType
import streetlight.model.data.LocationId
import streetlight.model.data.PostId
import streetlight.server.db.tables.EventStarTable
import streetlight.server.db.tables.GalaxyStarTable
import streetlight.server.db.tables.LocationStarTable
import streetlight.server.db.tables.PostMarkTable
import kotlin.time.Clock
import kotlin.uuid.Uuid

private class LightConfig(
    val lightTable: Table,
    val lightStarId: Column<EntityID<Uuid>>,
    val lightForeignId: Column<EntityID<Uuid>>,
    val createdAt: Column<Instant>,
)

class LightTableDao : DbService() {

    private val eventLight = LightConfig(
        lightTable = EventStarTable,
        lightStarId = EventStarTable.starId,
        lightForeignId = EventStarTable.eventId,
        createdAt = EventStarTable.createdAt,
    )

    private val galaxyLight = LightConfig(
        lightTable = GalaxyStarTable,
        lightStarId = GalaxyStarTable.starId,
        lightForeignId = GalaxyStarTable.galaxyId,
        createdAt = GalaxyStarTable.createdAt,
    )

    private val locationLight = LightConfig(
        lightTable = LocationStarTable,
        lightStarId = LocationStarTable.starId,
        lightForeignId = LocationStarTable.locationId,
        createdAt = LocationStarTable.createdAt,
    )

    private val postLight = LightConfig(
        lightTable = PostMarkTable,
        lightStarId = PostMarkTable.starId,
        lightForeignId = PostMarkTable.postId,
        createdAt = PostMarkTable.createdAt,
    )

    // -- public API --

    suspend fun readEventLights(callerId: CallerId) = readLights(eventLight, callerId) { EventId(it) }
    suspend fun readGalaxyLights(callerId: CallerId) = readLights(galaxyLight, callerId) { GalaxyId(it) }
    suspend fun readLocationLights(callerId: CallerId) = readLights(locationLight, callerId) { LocationId(it) }
    suspend fun readPostLights(callerId: CallerId) = readLights(postLight, callerId) { PostId(it) }

    suspend fun editLight(edit: LightEdit, callerId: CallerId) = when (edit.lightType) {
        LightType.Event -> editLight(eventLight, edit, callerId)
        LightType.Galaxy -> editLight(galaxyLight, edit, callerId)
        LightType.Location -> editLight(locationLight, edit, callerId)
        LightType.Post -> editLight(postLight, edit, callerId)
    }

    suspend fun editLights(edits: List<LightEdit>, callerId: CallerId): Boolean {
        edits.forEach { editLight(it, callerId) }
        return true
    }

    // -- generic engine --
    private suspend fun <T> readLights(config: LightConfig, callerId: CallerId, toId: (Uuid) -> T) = dbQuery {
        config.lightTable.select(config.lightForeignId)
            .where { config.lightStarId.eq(callerId) }
            .map { toId(it[config.lightForeignId].value) }
    }

    private suspend fun editLight(
        config: LightConfig,
        edit: LightEdit,
        callerId: CallerId,
    ) = dbQuery {
        when (edit.isLit) {
            true -> config.lightTable.insertIgnore {
                it[config.lightStarId] = callerId.value
                it[config.lightForeignId] = edit.targetId
                it[config.createdAt] = Clock.System.now()
            }
            else -> config.lightTable.deleteWhere {
                config.lightForeignId.eq(edit.targetId) and config.lightStarId.eq(callerId)
            }
        }
        true
    }
}