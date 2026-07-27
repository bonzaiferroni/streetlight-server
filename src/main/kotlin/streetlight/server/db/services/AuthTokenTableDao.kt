package streetlight.server.db.services

import io.github.oshai.kotlinlogging.KotlinLogging
import kampfire.model.HashedToken
import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
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

    suspend fun consumeAllUserTokens(starId: StarId) = dbQuery {
        AuthTokenTable.update({ AuthTokenTable.starId.eq(starId) }) {
            it[AuthTokenTable.consumedAt] = Clock.System.now()
        }
    }

    suspend inline fun readToken(hashedToken: HashedToken, tokenType: AuthTokenType) = dbQuery {
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

    @Deprecated("use consumeToken")
    suspend fun verifyEmail(authToken: AuthToken) = dbQuery {
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


}

private val log = KotlinLogging.logger(AuthTokenTableDao::class)