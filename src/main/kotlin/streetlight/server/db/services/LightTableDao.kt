package streetlight.server.db.services

import kotlin.time.Instant
import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.EventId
import streetlight.model.data.GalaxyId
import streetlight.model.data.LightEdit
import streetlight.model.data.LightType
import streetlight.model.data.LocationId
import streetlight.model.data.PostId
import streetlight.model.data.StarId
import streetlight.server.db.tables.EventLightTable
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.GalaxyLightTable
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.LocationLightTable
import streetlight.server.db.tables.PostLightTable
import java.util.UUID
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
        lightTable = EventLightTable,
        lightStarId = EventLightTable.starId,
        lightForeignId = EventLightTable.eventId,
        createdAt = EventLightTable.createdAt,
    )

    private val galaxyLight = LightConfig(
        lightTable = GalaxyLightTable,
        lightStarId = GalaxyLightTable.starId,
        lightForeignId = GalaxyLightTable.galaxyId,
        createdAt = GalaxyLightTable.createdAt,
    )

    private val locationLight = LightConfig(
        lightTable = LocationLightTable,
        lightStarId = LocationLightTable.starId,
        lightForeignId = LocationLightTable.locationId,
        createdAt = LocationLightTable.createdAt,
    )

    private val postLight = LightConfig(
        lightTable = PostLightTable,
        lightStarId = PostLightTable.starId,
        lightForeignId = PostLightTable.postId,
        createdAt = PostLightTable.createdAt,
    )

    // -- public API --

    suspend fun readEventLights(starId: StarId) = readLights(eventLight, starId) { EventId(it) }
    suspend fun readGalaxyLights(starId: StarId) = readLights(galaxyLight, starId) { GalaxyId(it) }
    suspend fun readLocationLights(starId: StarId) = readLights(locationLight, starId) { LocationId(it) }
    suspend fun readPostLights(starId: StarId) = readLights(postLight, starId) { PostId(it) }

    suspend fun editLight(edit: LightEdit, starId: StarId) = when (edit.lightType) {
        LightType.Event -> editLight(eventLight, edit, starId)
        LightType.Galaxy -> editLight(galaxyLight, edit, starId)
        LightType.Location -> editLight(locationLight, edit, starId)
        LightType.Post -> editLight(postLight, edit, starId)
    }

    suspend fun editLights(edits: List<LightEdit>, starId: StarId): Boolean {
        edits.forEach { editLight(it, starId) }
        return true
    }

    // -- generic engine --
    private suspend fun <T> readLights(config: LightConfig, starId: StarId, toId: (Uuid) -> T) = dbQuery {
        config.lightTable.select(config.lightForeignId)
            .where { config.lightStarId.eq(starId) }
            .map { toId(it[config.lightForeignId].value) }
    }

    private suspend fun editLight(
        config: LightConfig,
        edit: LightEdit,
        starId: StarId,
    ) = dbQuery {
        when (edit.isLit) {
            true -> config.lightTable.insertIgnore {
                it[config.lightStarId] = starId.value
                it[config.lightForeignId] = edit.targetId
                it[config.createdAt] = Clock.System.now()
            }
            else -> config.lightTable.deleteWhere {
                config.lightForeignId.eq(edit.targetId) and config.lightStarId.eq(starId)
            }
        }
        true
    }
}