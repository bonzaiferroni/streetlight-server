package streetlight.server.db.services

import kampfire.api.Username
import kampfire.model.thumb
import klutch.db.DbService
import klutch.db.read
import klutch.db.readById
import klutch.db.updateSingleWhere
import klutch.utils.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.model.data.StarEdit
import streetlight.model.data.StarId
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toStar
import streetlight.server.db.tables.updateRecord
import streetlight.server.utils.toRecordId
import kotlin.let

class StarTableDao: DbService() {

    suspend fun updateStar(
        starId: StarId,
        edit: StarEdit,
    ) = dbQuery {
        println(edit.username)
        StarTable.updateSingleWhere({ StarTable.id.eq(starId)}) {
            it.updateRecord(edit)
        }
        StarTable.readById(starId.value).toStar()
    }

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
        StarTable.select(StarTable.image).where { StarTable.id.eq(starId) }.firstOrNull()?.let {
            it[StarTable.image]?.variants?.thumb
        }
    }

    suspend fun readStar(starId: StarId) = dbQuery {
        StarTable.read { it.id.eq(starId) }.firstOrNull()?.toStar()
    }
}