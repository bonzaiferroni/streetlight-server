package streetlight.server.db.services

import kampfire.api.Username
import kampfire.api.toUsername
import kampfire.model.UserRole
import kampfire.model.thumb
import klutch.db.DbService
import klutch.db.readById
import klutch.db.updateSingleWhere
import klutch.utils.eq
import klutch.utils.eqIgnoreCase
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.model.data.StarEdit
import streetlight.model.data.StarId
import streetlight.server.db.tables.SavedImageSet
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toStar
import streetlight.server.db.tables.updateRecord
import streetlight.server.model.StarIdentity
import streetlight.server.utils.toRecordId
import kotlin.let

class StarTableDao: DbService() {

    suspend fun readByUsername(username: Username) = dbQuery {
        StarTable.selectAll()
            .where { StarTable.username.eq(username) }
            .map { it.toStar() }
            .firstOrNull()
    }

    suspend fun readIdByUsername(username: Username): StarId? = dbQuery {
        StarTable.select(StarTable.id)
            .where { StarTable.username.eq(username) }
            .firstOrNull()?.let { it[StarTable.id].toRecordId() }
    }

    suspend fun readThumb(starId: StarId) = dbQuery {
        StarTable.select(StarTable.images).where { StarTable.id.eq(starId) }.firstOrNull()?.let {
            it[StarTable.images]?.thumb
        }
    }

    suspend fun readStarPrincipal(userId: StarId) = dbQuery {
        StarTable.select(StarTable.username, StarTable.roles).where { StarTable.id.eq(userId) }.map {
            StarIdentity(
                starId = userId,
                username = it[StarTable.username].toUsername(),
                roles = it[StarTable.roles].map { role -> UserRole.valueOf(role) }.toSet()
            )
        }.firstOrNull()
    }

    suspend fun updateStar(
        starId: StarId,
        edit: StarEdit,
        imageSet: SavedImageSet?
    ) = dbQuery {
        println(edit.username)
        StarTable.updateSingleWhere({ StarTable.id.eq(starId)}) {
            it.updateRecord(edit, imageSet)
        }
        StarTable.readById(starId.value).toStar()
    }

}