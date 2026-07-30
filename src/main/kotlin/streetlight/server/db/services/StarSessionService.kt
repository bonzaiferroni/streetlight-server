package streetlight.server.db.services

import kampfire.api.Email
import kampfire.api.PasswordHash
import kampfire.api.LoginIdentity
import kampfire.api.TableId
import kampfire.api.TableUuid
import kampfire.api.Username
import kampfire.api.toEmail
import kampfire.api.toUsername
import kampfire.model.AccountType
import kampfire.model.HashedToken
import kampfire.model.PrivateInfo
import kampfire.model.Token
import kampfire.model.UserRecord
import kampfire.model.UserSeed
import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.db.model.Identity
import klutch.db.model.Session
import klutch.db.model.SessionId
import klutch.db.model.SessionIdentity
import klutch.db.readFirstOrNull
import klutch.db.services.SessionService
import klutch.server.GUEST_ACTIVITY_PERIOD
import klutch.server.hashToken
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.StarId
import streetlight.server.db.tables.SessionTable
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.toRoleSet
import streetlight.server.db.tables.toUserRecord
import streetlight.server.utils.toRecordId
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

class StarSessionService: DbService(), SessionService {
    override suspend fun createSession(
        userId: TableId<Uuid>,
        token: HashedToken,
        ttl: Duration,
        expiresAt: Instant,
    ) = dbQuery {
        val sessionId = SessionId(Uuid.random())
        SessionTable.insert {
            it[id] = sessionId.value
            it[starId] = userId.value
            it[tokenHash] = token.value
            it[ttlSeconds] = ttl.inWholeSeconds.toInt()
            it[createdAt] = Clock.System.now()
            it[this.expiresAt] = expiresAt
        }
        sessionId
    }

    override suspend fun deleteSessions(userId: TableUuid, sparedSessionId: SessionId?) = dbQuery {
        when (sparedSessionId) {
            null -> SessionTable.deleteWhere { SessionTable.starId.eq(userId) }
            else -> SessionTable.deleteWhere { SessionTable.starId.eq(userId) and SessionTable.id.neq(sparedSessionId.value) }
        }
    }

    override suspend fun deleteSession(token: Token) = dbQuery {
        val hashedToken = hashToken(token)
        SessionTable.deleteWhere { SessionTable.tokenHash.eq(hashedToken.value) }
    }

    override suspend fun createUserRecord(seed: UserSeed) = dbQuery {
        val now = Clock.System.now()
        val user = UserRecord(
            userId = StarId.random(),
            username = seed.request.username,
            passwordHash = seed.passwordHash,
            disabledPasswordHash = null,
            email = seed.request.email,
            roles = seed.roles.toSet(),
            accountType = seed.accountType,
            guestToken = seed.guestToken,
            activeAt = now,
            createdAt = now,
            updatedAt = now,
        )

        StarTable.insertAndGetId {
            it.createRecord(user)
        }.let { StarId(it.value) }
    }

    override suspend fun upgradeAccount(callerId: CallerId, passwordHash: PasswordHash, email: Email?) = dbQuery {
        StarTable.update({ StarTable.id.eq(callerId) and StarTable.accountType.eq(AccountType.Guest) }) {
            it[StarTable.passwordHash] = passwordHash.value
            it[StarTable.email] = email?.value
            it[StarTable.guestToken] = null
            it[StarTable.accountType] = AccountType.Registered
        } == 1
    }

    override suspend fun readIdByUsername(username: Username) = dbQuery {
        StarTable.select(StarTable.id).where { StarTable.username.eq(username) }
            .firstOrNull()?.getOrNull(StarTable.id)?.toRecordId<StarId>()
    }

    override suspend fun readByUsernameOrEmail(identity: LoginIdentity): UserRecord? = dbQuery {
        StarTable.readFirstOrNull {
            eqIdentity(identity)
        }?.toUserRecord()
    }

    override suspend fun readPrivateInfo(username: Username) = dbQuery {
        StarTable.select(StarTable.name, StarTable.email)
            .where { StarTable.username.eq(username) }
            .firstOrNull()
            ?.let { PrivateInfo(it[StarTable.name], it[StarTable.email]?.toEmail()) }
    }

    override suspend fun checkUsernameExists(username: Username) = dbQuery {
        StarTable.select(StarTable.username).where { StarTable.username.lowerCase().eq(username.value.lowercase()) }.any()
    }

    override suspend fun generateUsername() = "${getAdjective()}${getNoun()}".toUsername()

    override suspend fun readSessionIdentity(token: Token) = dbQuery {
        val hashedToken = hashToken(token)
        SessionTable.innerJoin(StarTable).select(identityColumns).where {
            SessionTable.tokenHash.eq(hashedToken.value) and SessionTable.expiresAt.greater(Clock.System.now())
        }.firstOrNull()?.let {
            SessionIdentity(
                Session(
                    sessionId = SessionId(it[SessionTable.id].value),
                    token = token,
                    ttlSeconds = it[SessionTable.ttlSeconds],
                    activeAt = it[StarTable.activeAt],
                    expiresAt = it[SessionTable.expiresAt],
                ),
                Identity(
                    callerId = CallerId(it[StarTable.id].value),
                    roles = it[StarTable.roles].toRoleSet(),
                    username = it[StarTable.username].toUsername(),
                    accountType = it[StarTable.accountType],
                ),
            )
        }
    }

    override suspend fun extendSession(session: Session) = dbQuery {
        val hashedToken = hashToken(session.token)
        val expiresAt = Clock.System.now() + session.ttlSeconds.seconds
        val updated = SessionTable.update({
            SessionTable.tokenHash.eq(hashedToken.value)
        }) {
            it[SessionTable.expiresAt] = expiresAt
        }
        if (updated == 0) throw IllegalStateException("Session not found")
        session.copy(expiresAt = expiresAt)
    }

    override suspend fun refreshActivity(callerId: CallerId) = dbQuery {
        StarTable.update({ StarTable.id.eq(callerId)}) {
            it[StarTable.activeAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun checkGuest(token: Token) = dbQuery {
        val hashedToken = hashToken(token)
        StarTable.select(StarTable.username).where {
            StarTable.guestToken.eq(hashedToken.value) and StarTable.accountType.eq(AccountType.Guest) and
                    StarTable.activeAt.greaterEq(Clock.System.now() - GUEST_ACTIVITY_PERIOD)
        }.singleOrNull()?.let {
            it[StarTable.username].toUsername()
        }
    }

    private fun getAdjective() = "TheWhole"
    private fun getNoun() = "Enchilada"
}

private val identityColumns = listOf(
    SessionTable.id,
    SessionTable.ttlSeconds,
    SessionTable.expiresAt,
    StarTable.username,
    StarTable.id,
    StarTable.roles,
    StarTable.activeAt,
    StarTable.accountType
)

private fun eqIdentity(identity: LoginIdentity) = when (identity) {
    is Email -> StarTable.email.lowerCase() eq identity.value.lowercase()
    is Username -> StarTable.username.lowerCase() eq identity.value.lowercase()
}
