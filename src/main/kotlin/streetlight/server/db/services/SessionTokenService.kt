package streetlight.server.db.services

import streetlight.server.db.DataService
import streetlight.server.db.models.SessionToken
import streetlight.server.db.tables.SessionTokenEntity
import streetlight.server.db.tables.SessionTokenTable
import streetlight.server.db.tables.fromData
import streetlight.server.db.tables.toData

class SessionTokenService : DataService<SessionToken, SessionTokenEntity>(
    SessionTokenEntity,
    SessionTokenEntity::fromData,
    SessionTokenEntity::toData
) {
    suspend fun findByToken(token: String): SessionToken? = dbQuery {
        SessionTokenEntity.find { SessionTokenTable.token eq token }.firstOrNull()?.toData()
    }
}