package streetlight.server.db.services

import kampfire.api.Email
import kampfire.api.PasswordHash
import kampfire.api.Username
import kampfire.model.CallerId
import kampfire.model.thumb
import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.Account
import streetlight.model.data.EmailStatus
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

    suspend fun setEmailStatus(email: Email, emailStatus: EmailStatus) = dbQuery {
        StarTable.update({ StarTable.email.eq(email.value) }) {
            it[StarTable.emailStatus] = emailStatus
        }
    }

    suspend fun setEmail(starId: StarId, email: Email, emailStatus: EmailStatus) = dbQuery {
        StarTable.update({ StarTable.id.eq(starId) }) {
            it[StarTable.email] = email.value
            it[StarTable.emailStatus] = emailStatus
        }
    }

    suspend fun setPassword(starId: StarId, passwordHash: PasswordHash) = dbQuery {
        StarTable.update({ StarTable.id.eq(starId) }) {
            it[StarTable.passwordHash] = passwordHash.value
        }
    }

    suspend fun setEmailNotOwned(starId: StarId) = dbQuery {
        StarTable.update({ StarTable.id.eq(starId) }) {
            it[StarTable.emailStatus] = EmailStatus.NotOwned
            it[StarTable.email] = null
        }
    }

    suspend fun disablePassword(starId: StarId) = dbQuery {
        StarTable.update({ StarTable.id.eq(starId) and StarTable.passwordHash.isNotNull() }) {
            it[StarTable.disabledPasswordHash] = StarTable.passwordHash
            it[StarTable.passwordHash] = null
        }
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

    suspend fun readAccount(starId: StarId) = dbQuery {
        StarTable.select(AccountQuery.columns).where {
            StarTable.id.eq(starId)
        }.singleOrNull()?.toAccount()
    }

    suspend fun readAccount(email: Email) = dbQuery {
        StarTable.select(AccountQuery.columns).where {
            StarTable.email.eq(email.value)
        }.singleOrNull()?.toAccount()
    }

    suspend fun readPasswordHash(callerId: CallerId) = dbQuery {
        StarTable.select(StarTable.passwordHash).where {
            StarTable.id.eq(callerId)
        }.singleOrNull()?.let { it[StarTable.passwordHash]?.let { value -> PasswordHash(value) } }
    }
}