package streetlight.server.db.services

import kampfire.model.thumb
import klutch.db.DbService
import klutch.db.readById
import klutch.db.updateSingleWhere
import klutch.utils.eq
import klutch.utils.eqLowercase
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.model.data.StarEdit
import streetlight.model.data.StarId
import streetlight.server.db.tables.SavedImageSet
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toStar
import streetlight.server.db.tables.writeUpdate

class StarTableDao: DbService() {

    suspend fun readByUsername(username: String) = dbQuery {
        StarTable.selectAll()
            .where { StarTable.username.eqLowercase(username) }
            .map { it.toStar() }
            .firstOrNull()
    }

    suspend fun readThumb(starId: StarId) = dbQuery {
        StarTable.select(StarTable.images).where { StarTable.id.eq(starId) }.firstOrNull()?.let {
            it[StarTable.images]?.thumb
        }
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