package streetlight.server.db.services

import kampfire.api.Username
import kampfire.api.toEmail
import kampfire.model.CallerId
import kampfire.model.PrivateInfo
import kampfire.model.thumb
import klutch.db.DbService
import klutch.db.read
import klutch.db.readById
import klutch.db.updateSingleWhere
import klutch.utils.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.CityId
import streetlight.model.data.IdentityInfo
import streetlight.model.data.StarEdit
import streetlight.model.data.StarId
import streetlight.server.db.tables.IdentityQuery
import streetlight.server.db.tables.StarQuery
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toIdentityInfo
import streetlight.server.db.tables.toStar
import streetlight.server.db.tables.updateRecord
import streetlight.server.utils.toRecordId
import kotlin.let

class StarTableDao: DbService() {

    suspend fun updateStar(
        callerId: CallerId,
        edit: StarEdit,
    ) = dbQuery {
        StarTable.updateReturning(where = { StarTable.id.eq(callerId)}) {
            it.updateRecord(edit)
        }.singleOrNull()?.toStar()
    }

    suspend fun readStar(username: Username, callerId: CallerId?) = dbQuery {
        // td: add starlight
        StarTable.select(StarQuery.columns)
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
        StarTable.select(StarTable.image).where { StarTable.id.eq(starId) }.singleOrNull()?.let {
            it[StarTable.image]?.variants?.thumb
        }
    }

    suspend fun readStar(starId: StarId) = dbQuery {
        StarTable.select(StarQuery.columns).where { StarTable.id.eq(starId) }.singleOrNull()?.toStar()
    }

    suspend fun readIdentityInfo(callerId: CallerId) = dbQuery {
        StarTable.select(IdentityQuery.columns).where {
            StarTable.id.eq(callerId)
        }.singleOrNull()?.toIdentityInfo()
    }
}