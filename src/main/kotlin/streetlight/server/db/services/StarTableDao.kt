package streetlight.server.db.services

import kampfire.api.Username
import kampfire.model.CallerId
import kampfire.model.thumb
import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.Account
import streetlight.model.data.StarEdit
import streetlight.model.data.StarId
import streetlight.server.db.tables.AccountQuery
import streetlight.server.db.tables.StarQuery
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toAccount
import streetlight.server.db.tables.toStar
import streetlight.server.db.tables.updateProfile
import streetlight.server.db.tables.updateAccount
import streetlight.server.utils.toRecordId
import kotlin.let

class StarTableDao: DbService() {

    suspend fun updateProfile(callerId: CallerId, edit: StarEdit) = dbQuery {
        StarTable.updateReturning(where = { StarTable.id.eq(callerId)}) {
            it.updateProfile(edit)
        }.singleOrNull()?.toStar()
    }

    suspend fun updateAccount(callerId: CallerId, edit: Account) = dbQuery {
        StarTable.update(where = { StarTable.id.eq(callerId) }) {
            it.updateAccount(edit)
        } == 1
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

    suspend fun readAccount(callerId: CallerId) = dbQuery {
        StarTable.select(AccountQuery.columns).where {
            StarTable.id.eq(callerId)
        }.singleOrNull()?.toAccount()
    }
}