package streetlight.server.db.dao

import kampfire.api.EmailAddress
import kampfire.api.PasswordHash
import kampfire.api.Username
import kampfire.model.thumb
import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.EmailStatus
import streetlight.model.data.StarEdit
import streetlight.model.data.StarId
import streetlight.server.db.tables.AccountQuery
import streetlight.server.db.tables.StarQuery
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toAccount
import streetlight.server.db.tables.toStar
import streetlight.server.db.tables.updateStar
import streetlight.server.utils.toRecordId
import kampfire.model.AccountType
import org.jetbrains.exposed.v1.core.andIfNotNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import streetlight.model.data.EntityCursor
import streetlight.server.db.tables.afterCursor
import streetlight.server.db.tables.containsPatternOf
import kotlin.let

class StarTableDao: DbService() {

    suspend fun updateProfile(callerId: CallerId, edit: StarEdit) = dbQuery {
        StarTable.updateReturning(where = { StarTable.id.eq(callerId)}) {
            it.updateStar(edit)
        }.singleOrNull()?.toStar()
    }

    suspend fun setEmailStatus(email: EmailAddress, emailStatus: EmailStatus) = dbQuery {
        StarTable.update({ StarTable.email.eq(email.value) }) {
            it[StarTable.emailStatus] = emailStatus
        }
    }

    suspend fun setEmail(starId: StarId, email: EmailAddress?, emailStatus: EmailStatus?) = dbQuery {
        StarTable.update({ StarTable.id.eq(starId) }) {
            it[StarTable.email] = email?.value
            it[StarTable.emailStatus] = emailStatus
        }
    }

    suspend fun setEmailNotOwned(starId: StarId) = dbQuery {
        StarTable.update({ StarTable.id.eq(starId) }) {
            it[StarTable.emailStatus] = EmailStatus.NotOwned
            it[StarTable.email] = null
        }
    }

    suspend fun setPassword(starId: StarId, passwordHash: PasswordHash) = dbQuery {
        StarTable.update({ StarTable.id.eq(starId) }) {
            it[StarTable.passwordHash] = passwordHash.value
        }
    }

    suspend fun disablePassword(starId: StarId) = dbQuery {
        StarTable.update({ StarTable.id.eq(starId) and StarTable.passwordHash.isNotNull() }) {
            it[StarTable.disabledPasswordHash] = StarTable.passwordHash
            it[StarTable.passwordHash] = null
        }
    }

    suspend fun readStar(username: Username, callerId: CallerId?, withDesign: Boolean = false) = dbQuery {
        // td: add starlight
        StarTable.select(StarQuery.getColumns(withDesign))
            .where { StarTable.username.eq(username) }
            .map { it.toStar() }
            .firstOrNull()
    }

    /**
     * The page at [cursor] of every registered star by joining, newest first, holding its search, each paired with its
     * id for the next cursor.
     */
    suspend fun readNewStars(cursor: EntityCursor.Time) = dbQuery {
        val after = cursor.recordAt?.let { recordAt ->
            cursor.recordId?.let { afterCursor(StarTable.createdAt, recordAt, StarTable.id, it, cursor.direction) }
        }
        StarTable.select(StarQuery.columns + StarTable.id)
            .where { StarTable.accountType.eq(AccountType.Registered) andIfNotNull after andIfNotNull starHolds(cursor.search) }
            .orderBy(StarTable.createdAt to cursor.direction.toSortOrder(), StarTable.id to cursor.direction.toSortOrder())
            .limit(EntityCursor.DefaultLimit)
            .map { it[StarTable.id].value to it.toStar() }
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

    suspend fun readAccount(email: EmailAddress) = dbQuery {
        StarTable.select(AccountQuery.columns).where {
            StarTable.email.eq(email.value)
        }.singleOrNull()?.toAccount()
    }

    suspend fun readDesign(callerId: CallerId) = dbQuery {
        StarTable.select(StarTable.design).where { StarTable.id.eq(callerId) }
            .singleOrNull()?.let { it[StarTable.design] }
    }

    suspend fun readPasswordHash(starId: StarId) = dbQuery {
        StarTable.select(StarTable.passwordHash).where {
            StarTable.id.eq(starId)
        }.singleOrNull()?.let { it[StarTable.passwordHash]?.let { value -> PasswordHash(value) } }
    }
}

/** The condition that a star's username or tagline holds [search], or `null` without a search. */
private fun starHolds(search: String?) = containsPatternOf(search)?.let { pattern ->
    StarTable.username.lowerCase().like(pattern) or StarTable.tagline.lowerCase().like(pattern)
}
