package streetlight.server.db.services

import kampfire.api.TableId
import kampfire.api.TableUuid
import kampfire.api.Username
import kampfire.api.toUsername
import kampfire.model.HashedToken
import kampfire.model.PrivateInfo
import kampfire.model.Session
import kampfire.model.SessionIdentity
import kampfire.model.Token
import kampfire.model.UserRecord
import kampfire.model.UserSeed
import klutch.db.DbService
import klutch.db.readFirstOrNull
import klutch.db.services.SessionService
import klutch.server.hashToken
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.StarId
import streetlight.model.data.StarRecord
import streetlight.server.db.tables.SessionTable
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.toUserRecord
import streetlight.server.model.StarIdentity
import streetlight.server.utils.toRecordId
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

class StarSessionService: DbService(), SessionService {
    override suspend fun createSessionRecord(
        userId: TableId<Uuid>,
        token: HashedToken,
        ttl: Duration,
        expiresAt: Instant,
    ) = dbQuery {
        SessionTable.insert {
            it[id] = Uuid.random()
            it[starId] = userId.value
            it[tokenHash] = token.hash
            it[ttlSeconds] = ttl.inWholeSeconds.toInt()
            it[createdAt] = Clock.System.now()
            it[this.expiresAt] = expiresAt
        }
        true
    }

    override suspend fun deleteSessions(userId: TableUuid) = dbQuery {
        SessionTable.deleteWhere { SessionTable.starId.eq(userId) }
    }

    override suspend fun deleteSession(token: Token) = dbQuery {
        val hashedToken = hashToken(token)
        SessionTable.deleteWhere { SessionTable.tokenHash.eq(hashedToken.hash) }
    }

    override suspend fun createUserRecord(seed: UserSeed) = dbQuery {
        val now = Clock.System.now()
        val user = StarRecord(
            starId = StarId.random(),
            username = seed.request.username,
            hashedPassword = seed.hashedPassword,
            salt = seed.salt,
            email = seed.request.email,
            roles = seed.roles.toSet(),
            createdAt = now,
            updatedAt = now,
        )

        StarTable.insertAndGetId {
            it.createRecord(user, seed.accountType)
        }.let { StarId(it.value) }
    }

    override suspend fun readIdByUsername(username: Username) = dbQuery {
        StarTable.select(StarTable.id).where { StarTable.username.eq(username) }
            .firstOrNull()?.getOrNull(StarTable.id)?.toRecordId<StarId>()
    }

    override suspend fun readByUsernameOrEmail(identity: String): UserRecord? = dbQuery {
        StarTable.readFirstOrNull {
            eqIdentity(identity)
        }?.toUserRecord()
    }

    override suspend fun readPrivateInfo(username: Username) = dbQuery {
        StarTable.select(StarTable.name, StarTable.email)
            .where { StarTable.username.eq(username) }
            .firstOrNull()
            ?.let { PrivateInfo(it[StarTable.name], it[StarTable.email]) }
    }

    override suspend fun readSaltExists(salt: String) = dbQuery {
        StarTable
            .select(StarTable.salt)
            .where { StarTable.salt.eq(salt) }
            .firstOrNull() != null
    }

    override suspend fun generateUsername() = "${getAdjective()}${getNoun()}".toUsername()

    override suspend fun readSessionIdentity(token: Token) = dbQuery {
        val hashedToken = hashToken(token)
        SessionTable.innerJoin(StarTable).select(identityColumns).where {
            SessionTable.tokenHash.eq(hashedToken.hash) and SessionTable.expiresAt.greater(Clock.System.now())
        }.firstOrNull()?.let {
            SessionIdentity(
                Session(
                    token = token,
                    ttlSeconds = it[SessionTable.ttlSeconds],
                    expiresAt = it[SessionTable.expiresAt],
                ),
                StarIdentity(
                    starId = StarId(it[StarTable.id].value),
                    roles = it[StarTable.roles],
                    username = it[StarTable.username].toUsername(),
                ),
            )
        }
    }

    override suspend fun extendSession(session: Session) = dbQuery {
        val hashedToken = hashToken(session.token)
        val expiresAt = Clock.System.now() + session.ttlSeconds.seconds
        val updated = SessionTable.update({
            SessionTable.tokenHash.eq(hashedToken.hash)
        }) {
            it[SessionTable.expiresAt] = expiresAt
        }
        if (updated == 0) throw IllegalStateException("Session not found")
        Session(session.token, session.ttlSeconds, expiresAt)
    }

    private fun getAdjective() = "TheWhole"
    private fun getNoun() = "Enchilada"
}

private val identityColumns = listOf(
    SessionTable.ttlSeconds,
    SessionTable.expiresAt,
    StarTable.username,
    StarTable.id,
    StarTable.roles,
)

private fun eqIdentity(identity: String) =
    (StarTable.username.lowerCase() eq identity.lowercase()) or (StarTable.email.lowerCase() eq identity.lowercase())