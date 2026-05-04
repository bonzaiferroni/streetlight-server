package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.and
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
import streetlight.model.data.StarId
import streetlight.server.db.tables.EventLightTable
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.GalaxyLightTable
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.utils.toProjectId
import kotlin.time.Clock

class LightTableDaoProto: DbService() {

    suspend fun readEventLights(starId: StarId) = dbQuery {
        EventLightTable.read { EventLightTable.starId.eq(starId) }.map { it[EventLightTable.eventId].toProjectId<EventId>() }
    }

    suspend fun editEventLight(edit: LightEdit, starId: StarId) = dbQuery {
        val eventId = EventId(edit.stringId)
        when (edit.isLit) {
            true -> EventLightTable.insertIgnore {
                it[this.starId] = starId.toUUID()
                it[this.eventId] = eventId.toUUID()
                it[this.createdAt] = Clock.System.now()
            }
            else -> EventLightTable.deleteWhere {
                this.eventId.eq(edit.stringId) and this.starId.eq(starId)
            }
        }
        refreshEventLightCount(eventId)
        true
    }

    suspend fun editEventLights(edits: List<LightEdit>, starId: StarId) = dbQuery {
        val (toLight, toUnlight) = edits.partition { it.isLit }

        val existingIds = EventTable
            .select(EventTable.id)
            .where { EventTable.id inList toLight.map { it.stringId.toUUID() } }
            .map { it[EventTable.id].value }
            .toSet()

        EventLightTable.batchInsert(toLight.filter { it.stringId.toUUID() in existingIds }, ignore = true) {
            this[EventLightTable.starId] = starId.toUUID()
            this[EventLightTable.eventId] = it.stringId.toUUID()
            this[EventLightTable.createdAt] = Clock.System.now()
        }

        toUnlight.forEach { edit ->
            EventLightTable.deleteWhere {
                this.eventId.eq(edit.stringId) and this.starId.eq(starId)
            }
        }

        true
    }

    suspend fun editGalaxyLight(edit: LightEdit, starId: StarId) = dbQuery {
        val galaxyId = GalaxyId(edit.stringId)
        when (edit.isLit) {
            true -> GalaxyLightTable.insertIgnore {
                it[this.starId] = starId.toUUID()
                it[this.galaxyId] = galaxyId.toUUID()
                it[this.createdAt] = Clock.System.now()
            }
            else -> GalaxyLightTable.deleteWhere {
                this.galaxyId.eq(edit.stringId) and this.starId.eq(starId)
            }
        }
        refreshGalaxyLightCount(galaxyId)
        true
    }

    suspend fun editGalaxyLights(edits: List<LightEdit>, starId: StarId) = dbQuery {
        val (toLight, toUnlight) = edits.partition { it.isLit }

        val existingIds = GalaxyTable
            .select(GalaxyTable.id)
            .where { GalaxyTable.id inList toLight.map { it.stringId.toUUID() } }
            .map { it[GalaxyTable.id].value }
            .toSet()

        GalaxyLightTable.batchInsert(toLight.filter { it.stringId.toUUID() in existingIds }, ignore = true) {
            this[GalaxyLightTable.starId] = starId.toUUID()
            this[GalaxyLightTable.galaxyId] = it.stringId.toUUID()
            this[GalaxyLightTable.createdAt] = Clock.System.now()
        }

        toUnlight.forEach { edit ->
            GalaxyLightTable.deleteWhere {
                this.galaxyId.eq(edit.stringId) and this.starId.eq(starId)
            }
        }

        true
    }

    private fun refreshGalaxyLightCount(galaxyId: GalaxyId) {
        val count = GalaxyLightTable.selectAll()
            .where { GalaxyLightTable.galaxyId.eq(galaxyId) }
            .count().toInt()
        GalaxyTable.update({ GalaxyTable.id.eq(galaxyId) }) {
            it[this.lightCount] = count
        }
    }

    private fun refreshEventLightCount(eventId: EventId) {
        val count = EventLightTable.selectAll()
            .where { EventLightTable.eventId.eq(eventId) }
            .count().toInt()
        EventTable.update({ EventTable.id.eq(eventId) }) {
            it[this.lightCount] = count
        }
    }
}