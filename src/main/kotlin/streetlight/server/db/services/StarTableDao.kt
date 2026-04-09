package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.readById
import klutch.db.updateSingleWhere
import klutch.utils.eq
import klutch.utils.eqLowercase
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.model.data.EventEdit
import streetlight.model.data.EventId
import streetlight.model.data.LightEdit
import streetlight.model.data.StarEdit
import streetlight.model.data.StarId
import streetlight.server.db.tables.EventLightTable
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.SavedImageSet
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toEvent
import streetlight.server.db.tables.toStar
import streetlight.server.db.tables.writeUpdate
import kotlin.time.Clock

class StarTableDao: DbService() {

    suspend fun readByUsername(username: String) = dbQuery {
        StarTable.selectAll()
            .where { StarTable.username.eqLowercase(username) }
            .map { it.toStar() }
            .firstOrNull()
    }

    suspend fun updateStar(
        starId: StarId,
        edit: StarEdit,
        imageSet: SavedImageSet?
    ) = dbQuery {
        StarTable.updateSingleWhere({ StarTable.id.eq(starId)}) {
            it.writeUpdate(edit, imageSet)
        }
        StarTable.readById(starId.toUUID()).toStar()
    }

}