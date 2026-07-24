package streetlight.server.db.tables

import kampfire.api.Email
import kampfire.model.HashedToken
import klutch.db.jsonColumnConfig
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.StarId
import streetlight.server.utils.toRecordId
import kotlin.time.Instant

object AuthTokenTable: LongIdTable("auth_token") {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE)
    val hashedToken = char("hashed_token", 64).uniqueIndex()
    val authType = enumeration<AuthType>("auth_type")
    val meta = jsonb<AuthMeta>("meta", jsonColumnConfig)
    val consumedAt = timestamp("consumed_at").nullable()
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
}

enum class AuthType {
    EmailVerification,
    PasswordReset,
}

@Serializable
sealed interface AuthMeta {
    @Serializable
    data class EmailVerification(val email: Email): AuthMeta

    @Serializable
    object Null: AuthMeta
}

fun <T: AuthMeta> UpdateBuilder<*>.createToken(authToken: AuthToken<T>) {
    this[AuthTokenTable.starId] = authToken.starId.value
    this[AuthTokenTable.hashedToken] = authToken.hashedToken.value
    this[AuthTokenTable.authType] = authToken.authType
    this[AuthTokenTable.meta] = authToken.meta
    this[AuthTokenTable.consumedAt] = authToken.consumedAt
    this[AuthTokenTable.expiresAt] = authToken.expiresAt
    this[AuthTokenTable.createdAt] = authToken.createdAt
}

data class AuthToken<T: AuthMeta>(
    val tokenId: Long,
    val starId: StarId,
    val hashedToken: HashedToken,
    val authType: AuthType,
    val meta: T,
    val consumedAt: Instant?,
    val expiresAt: Instant,
    val createdAt: Instant,
)

inline fun <reified T: AuthMeta> ResultRow.toAuthToken() = AuthToken(
    tokenId = this[AuthTokenTable.id].value,
    starId = this[AuthTokenTable.starId].toRecordId(),
    hashedToken = HashedToken(this[AuthTokenTable.hashedToken]),
    authType = this[AuthTokenTable.authType],
    meta = this[AuthTokenTable.meta] as T,
    consumedAt = this[AuthTokenTable.consumedAt],
    expiresAt = this[AuthTokenTable.expiresAt],
    createdAt = this[AuthTokenTable.createdAt]
)