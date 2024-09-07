package streetlight.server.db.services

import streetlight.server.db.models.SessionToken
import streetlight.server.db.DataService
import streetlight.server.db.tables.SessionTokenEntity
import streetlight.server.db.tables.SessionTokenTable
import streetlight.server.db.tables.UserEntity

class SessionTokenService : DataService<SessionToken, SessionTokenEntity>(SessionTokenEntity) {
    override suspend fun createEntity(data: SessionToken): (SessionTokenEntity.() -> Unit)? {
        val user = UserEntity.findById(data.userId) ?: return null
        return {
            this.user = user
            token = data.token
            createdAt = data.createdAt
            expiresAt = data.expiresAt
            issuer = data.issuer
        }
    }

    override fun SessionTokenEntity.toData() = SessionToken(
        id.value,
        user.id.value,
        token,
        createdAt,
        expiresAt,
        issuer,
    )

    override suspend fun updateEntity(data: SessionToken): ((SessionTokenEntity) -> Unit)? {
        val user = UserEntity.findById(data.userId) ?: return null
        return {
            it.user = user
            it.token = data.token
            it.createdAt = data.createdAt
            it.expiresAt = data.expiresAt
            it.issuer = data.issuer
        }
    }

    suspend fun findByToken(token: String): SessionToken? = dbQuery {
        SessionTokenEntity.find { SessionTokenTable.token eq token }.firstOrNull()?.toData()
    }
}