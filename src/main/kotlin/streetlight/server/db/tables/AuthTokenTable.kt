package streetlight.server.db.tables

import kampfire.api.Email
import kampfire.api.toEmail
import kampfire.model.HashedToken
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.StarId
import streetlight.server.utils.toRecordId
import kotlin.time.Instant

object AuthTokenTable: LongIdTable("auth_token") {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE)
    val hashedToken = char("hashed_token", 64).uniqueIndex()
    val tokenType = enumeration<AuthTokenType>("auth_type")
    val email = text("email")
    val consumedAt = timestamp("consumed_at").nullable()
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
}

enum class AuthTokenType {
    EmailVerification,
    PasswordReset,
    AccountLockdown,
    AccountNotOwned,
}

fun UpdateBuilder<*>.createToken(authToken: AuthToken) {
    this[AuthTokenTable.starId] = authToken.starId.value
    this[AuthTokenTable.hashedToken] = authToken.hashedToken.value
    this[AuthTokenTable.tokenType] = authToken.tokenType
    this[AuthTokenTable.email] = authToken.email.value
    this[AuthTokenTable.consumedAt] = authToken.consumedAt
    this[AuthTokenTable.expiresAt] = authToken.expiresAt
    this[AuthTokenTable.createdAt] = authToken.createdAt
}

data class AuthToken(
    val tokenId: Long,
    val starId: StarId,
    val hashedToken: HashedToken,
    val tokenType: AuthTokenType,
    val email: Email,
    val consumedAt: Instant?,
    val expiresAt: Instant,
    val createdAt: Instant,
)

fun ResultRow.toAuthToken() = AuthToken(
    tokenId = this[AuthTokenTable.id].value,
    starId = this[AuthTokenTable.starId].toRecordId(),
    hashedToken = HashedToken(this[AuthTokenTable.hashedToken]),
    tokenType = this[AuthTokenTable.tokenType],
    email = this[AuthTokenTable.email].toEmail(),
    consumedAt = this[AuthTokenTable.consumedAt],
    expiresAt = this[AuthTokenTable.expiresAt],
    createdAt = this[AuthTokenTable.createdAt]
)