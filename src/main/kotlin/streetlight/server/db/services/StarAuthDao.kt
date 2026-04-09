package streetlight.server.db.services

import kampfire.model.PrivateInfo
import kampfire.model.UserSeed
import klutch.db.DbService
import klutch.db.readFirstOrNull
import klutch.db.services.AuthDao
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.StarId
import streetlight.model.data.StarUser
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toStarUser
import streetlight.server.db.tables.writeFull
import streetlight.server.utils.toProjectId
import kotlin.time.Clock

class StarAuthDao: AuthDao<StarUser, StarId>, DbService() {
    override suspend fun createUser(user: StarUser) = dbQuery {
        StarTable.insertAndGetId {
            it.writeFull(user)
        }.value
    }

    override suspend fun readIdByUsername(username: String) = dbQuery {
        StarTable.select(StarTable.id).where { StarTable.username.eq(username) }
            .firstOrNull()?.getOrNull(StarTable.id)?.toProjectId<StarId>()
    }

    override suspend fun readByUsernameOrEmail(identity: String): StarUser? = dbQuery {
        StarTable.readFirstOrNull {
            eqIdentity(identity)
        }?.toStarUser()
    }

    override suspend fun readPrivateInfo(identity: String) = dbQuery {
        StarTable.select(StarTable.name, StarTable.email)
            .where { eqIdentity(identity) }
            .firstOrNull()
            ?.let { PrivateInfo(it[StarTable.name], it[StarTable.email]) }
    }

    override suspend fun readSaltExists(salt: String) = dbQuery {
        StarTable
            .select(StarTable.salt)
            .where { StarTable.salt.eq(salt) }
            .firstOrNull() != null
    }
}

private fun eqIdentity(identity: String) =
    (StarTable.username.lowerCase() eq identity.lowercase()) or (StarTable.email.lowerCase() eq identity.lowercase())

fun provideStarUser(seed: UserSeed): StarUser {
    val now = Clock.System.now()
    return StarUser(
        starId = StarId.random(),
        username = seed.request.username,
        hashedPassword = seed.hashedPassword,
        salt = seed.salt,
        email = seed.request.email,
        roles = seed.roles.toSet(),
        createdAt = now,
        updatedAt = now,
    )
}