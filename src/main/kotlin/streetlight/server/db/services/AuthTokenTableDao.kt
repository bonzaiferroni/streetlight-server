package streetlight.server.db.services

import io.github.oshai.kotlinlogging.KotlinLogging
import kampfire.api.Email
import kampfire.model.HashedToken
import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.AuthTokenType
import streetlight.model.data.EmailStatus
import streetlight.model.data.StarId
import streetlight.server.db.tables.AuthToken
import streetlight.server.db.tables.AuthTokenTable
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.createToken
import streetlight.server.db.tables.toAuthToken
import streetlight.server.plugins.logger
import kotlin.time.Clock
import kotlin.time.Instant

class AuthTokenTableDao: DbService() {
    suspend fun createToken(token: AuthToken) = dbQuery {
        AuthTokenTable.insert {
            it.createToken(token)
        }
    }

    suspend fun consumeAllTokensOfType(starId: StarId, tokenType: AuthTokenType) = dbQuery {
        AuthTokenTable.update({ AuthTokenTable.starId.eq(starId) and AuthTokenTable.tokenType.eq(tokenType) }) {
            it[AuthTokenTable.consumedAt] = Clock.System.now()
        }
    }

    suspend fun consumeAllUserTokens(starId: StarId, exceptLockdownBefore: Long) = dbQuery {
        AuthTokenTable.update({
            AuthTokenTable.starId.eq(starId) and
                    AuthTokenTable.consumedAt.isNull() and
                    not(
                        AuthTokenTable.tokenType.eq(AuthTokenType.AccountLockdown) and
                                AuthTokenTable.id.less(exceptLockdownBefore)
                    )
        }) {
            it[AuthTokenTable.consumedAt] = Clock.System.now()
        }
    }

    suspend fun consumeAllTokensForEmail(starId: StarId, email: Email) = dbQuery {
        AuthTokenTable.update({
            AuthTokenTable.starId.eq(starId) and
                    AuthTokenTable.email.eq(email.value) and
                    AuthTokenTable.consumedAt.isNull() and
                    AuthTokenTable.tokenType.neq(AuthTokenType.AccountLockdown)
        }) {
            it[AuthTokenTable.consumedAt] = Clock.System.now()
        }
    }

    suspend fun readToken(hashedToken: HashedToken, tokenType: AuthTokenType) = dbQuery {
        AuthTokenTable.selectAll()
            .where { AuthTokenTable.hashedToken.eq(hashedToken.value) and AuthTokenTable.tokenType.eq(tokenType) }
            .singleOrNull()?.toAuthToken()
    }

    suspend fun consumeToken(tokenId: Long, instant: Instant) = dbQuery {
        AuthTokenTable.update({
            AuthTokenTable.id.eq(tokenId) and AuthTokenTable.consumedAt.isNull()
        }) {
            it[AuthTokenTable.consumedAt] = instant
        }
    }

    suspend fun readIsVerifyEmailTokenActive(callerId: CallerId) = dbQuery {
        AuthTokenTable.selectAll().where {
            AuthTokenTable.starId.eq(callerId) and AuthTokenTable.tokenType.eq(AuthTokenType.EmailVerification) and
                    AuthTokenTable.consumedAt.isNull()
        }.any()
    }
}

private val log = KotlinLogging.logger(AuthTokenTableDao::class)