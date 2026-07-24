package streetlight.server.db.services

import io.github.oshai.kotlinlogging.KotlinLogging
import kampfire.api.Email
import kampfire.model.CallerId
import kampfire.model.HashedToken
import kampfire.model.Token
import klutch.db.DbService
import klutch.server.hashToken
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.EmailStatus
import streetlight.server.db.tables.AccountQuery
import streetlight.server.db.tables.AuthMeta
import streetlight.server.db.tables.AuthToken
import streetlight.server.db.tables.AuthTokenTable
import streetlight.server.db.tables.AuthType
import streetlight.server.db.tables.BouncedEmailTable
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.createToken
import streetlight.server.db.tables.toAccount
import streetlight.server.db.tables.toAuthToken
import streetlight.server.plugins.logger
import kotlin.time.Clock
import kotlin.time.Instant

class AuthTokenTableDao: DbService() {
    suspend fun <T: AuthMeta> createToken(token: AuthToken<T>) = dbQuery {
        AuthTokenTable.insert {
            it.createToken(token)
        }
    }

    suspend fun createBouncedEmail(email: Email, reason: String) = dbQuery {
        BouncedEmailTable.insert {
            it[BouncedEmailTable.email] = email.value
            it[BouncedEmailTable.reason] = reason
            it[BouncedEmailTable.bouncedAt] = Clock.System.now()
        }
    }

    suspend fun consumeAllTokensOfType(callerId: CallerId, authType: AuthType) = dbQuery {
        AuthTokenTable.update({ AuthTokenTable.starId.eq(callerId) and AuthTokenTable.authType.eq(authType) }) {
            it[AuthTokenTable.consumedAt] = Clock.System.now()
        }
    }

    suspend inline fun <reified T: AuthMeta> readToken(hashedToken: HashedToken, authType: AuthType) = dbQuery {
        AuthTokenTable.selectAll()
            .where { AuthTokenTable.hashedToken.eq(hashedToken.value) and AuthTokenTable.authType.eq(authType) }
            .singleOrNull()?.toAuthToken<T>()
    }

    suspend fun verifyEmail(authToken: AuthToken<AuthMeta.EmailVerification>) = dbQuery {
        var updatedCount = AuthTokenTable.update({
            AuthTokenTable.id.eq(authToken.tokenId) and AuthTokenTable.consumedAt.isNull()
        }) {
            it[AuthTokenTable.consumedAt] = Clock.System.now()
        }
        if (updatedCount != 1) error("unexpected update count: $updatedCount")
        updatedCount = StarTable.update({ StarTable.id.eq(authToken.starId)}) {
            it[StarTable.emailStatus] = EmailStatus.Verified
        }
        if (updatedCount != 1) error("unexpected update count: $updatedCount")
    }

    suspend fun readIsBounced(email: Email) = dbQuery {
        BouncedEmailTable.selectAll().where { BouncedEmailTable.email.eq(email.value) }.any() // is any the right function?
    }
}

private val log = KotlinLogging.logger(AuthTokenTableDao::class)