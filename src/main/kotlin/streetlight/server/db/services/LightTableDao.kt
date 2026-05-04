package streetlight.server.db.services

import kotlin.time.Instant
import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import klutch.utils.toStringId
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.EventId
import streetlight.model.data.GalaxyId
import streetlight.model.data.LightEdit
import streetlight.model.data.LightType
import streetlight.model.data.StarId
import streetlight.server.db.tables.EventLightTable
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.GalaxyLightTable
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.utils.toProjectId
import java.util.UUID
import kotlin.time.Clock

private class LightConfig(
    val parentTable: Table,
    val parentId: Column<EntityID<UUID>>,
    val lightTable: Table,
    val lightStarId: Column<EntityID<UUID>>,
    val lightForeignId: Column<EntityID<UUID>>,
    val parentLightCount: Column<Int>,
    val createdAt: Column<Instant>,
)

class LightTableDao : DbService() {

    private val eventLight = LightConfig(
        parentTable = EventTable,
        parentId = EventTable.id,
        lightTable = EventLightTable,
        lightStarId = EventLightTable.starId,
        lightForeignId = EventLightTable.eventId,
        parentLightCount = EventTable.lightCount,
        createdAt = EventLightTable.createdAt,
    )

    private val galaxyLight = LightConfig(
        parentTable = GalaxyTable,
        parentId = GalaxyTable.id,
        lightTable = GalaxyLightTable,
        lightStarId = GalaxyLightTable.starId,
        lightForeignId = GalaxyLightTable.galaxyId,
        parentLightCount = GalaxyTable.lightCount,
        createdAt = GalaxyLightTable.createdAt,
    )

    // -- public API --

    suspend fun readEventLights(starId: StarId) = readLights(eventLight, starId) { EventId(it) }
    suspend fun readGalaxyLights(starId: StarId) = readLights(galaxyLight, starId) { GalaxyId(it) }

    suspend fun editLight(edit: LightEdit, starId: StarId) = when (edit.lightType) {
        LightType.Event -> editLight(eventLight, edit, starId)
        LightType.Galaxy -> editLight(galaxyLight, edit, starId)
        LightType.Location -> TODO()
    }

    suspend fun editLights(edits: List<LightEdit>, starId: StarId): Boolean {
        edits.forEach { editLight(it, starId) }
        return true
    }

    // -- generic engine --
    private suspend fun <T> readLights(config: LightConfig, starId: StarId, toId: (String) -> T) = dbQuery {
        config.lightTable
            .read { config.lightStarId.eq(starId) }
            .map { toId(it[EventLightTable.eventId].value.toStringId()) }
    }

    private suspend fun editLight(
        config: LightConfig,
        edit: LightEdit,
        starId: StarId,
    ) = dbQuery {
        when (edit.isLit) {
            true -> config.lightTable.insertIgnore {
                it[config.lightStarId] = starId.toUUID()
                it[config.lightForeignId] = edit.stringId.toUUID()
                it[config.createdAt] = Clock.System.now()
            }
            else -> config.lightTable.deleteWhere {
                config.lightForeignId.eq(edit.stringId) and config.lightStarId.eq(starId)
            }
        }
        refreshLightCount(config, edit.stringId)
        true
    }

    private fun refreshLightCount(config: LightConfig, foreignId: String) {
        val count = config.lightTable.selectAll()
            .where { config.lightForeignId.eq(foreignId) }
            .count().toInt()
        config.parentTable.update({ config.parentId.eq(foreignId) }) {
            it[config.parentLightCount] = count
        }
    }
}