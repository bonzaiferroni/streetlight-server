package streetlight.server.db.services

import kampfire.api.TableId
import kampfire.api.TableUuid
import kampfire.api.Username
import kampfire.api.toUsername
import kampfire.model.HashedToken
import kampfire.model.PrivateInfo
import kampfire.model.SessionPrincipal
import kampfire.model.UserRecord
import kampfire.model.UserSeed
import klutch.db.DbService
import klutch.db.readFirstOrNull
import klutch.db.services.SessionService
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
import kotlin.uuid.Uuid

class StarSessionService: DbService(), SessionService {
    // suspend fun readToken(value: String): RefreshToken? = dbQuery {
    //     table.select(table.columns)
    //         .where { table.token eq value }
    //         .firstOrNull()?.toSessionToken(table)
    // }

    override suspend fun createSessionRecord(
        userId: TableId<Uuid>,
        token: HashedToken,
        isTemp: Boolean,
        ttl: Duration
    ) = dbQuery {
        SessionTable.insert {
            it[id] = Uuid.random()
            it[starId] = userId.value
            it[tokenHash] = token.value
            it[this.isTemp] = isTemp
            it[createdAt] = Clock.System.now()
            it[expiresAt] = Clock.System.now() + ttl
        }
        true
    }

    override suspend fun deleteSessions(userId: TableUuid) = dbQuery {
        SessionTable.deleteWhere { SessionTable.starId.eq(userId) }
    }

    override suspend fun deleteSession(token: HashedToken) = dbQuery {
        SessionTable.deleteWhere { SessionTable.tokenHash.eq(token.value) }
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

    override suspend fun readSessionPrincipal(token: HashedToken) = dbQuery {
        SessionTable.leftJoin(StarTable).select(identityColumns).where {
            SessionTable.tokenHash.eq(token.value) and SessionTable.expiresAt.greater(Clock.System.now())
        }.firstOrNull()?.let {
            SessionPrincipal(
                StarIdentity(
                    starId = StarId(it[StarTable.id].value),
                    roles = it[StarTable.roles],
                    username = it[StarTable.username].toUsername(),
                    token = token,
                ),
                createdAt = it[SessionTable.createdAt],
                expiresAt = it[SessionTable.expiresAt],
            )
        }
    }

    private fun getAdjective() = "TheWhole"
    private fun getNoun() = "Enchilada"
}

private val identityColumns = listOf(
    SessionTable.tokenHash,
    SessionTable.createdAt,
    SessionTable.expiresAt,
    StarTable.username,
    StarTable.id,
    StarTable.roles,
)

private fun eqIdentity(identity: String) =
    (StarTable.username.lowerCase() eq identity.lowercase()) or (StarTable.email.lowerCase() eq identity.lowercase())