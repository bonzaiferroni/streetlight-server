package streetlight.server.db.models

import streetlight.model.core.IdModel

data class SessionToken(
    override val id: Int = 0,
    val userId: Int = 0,
    val token: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val issuer: String = "",
): IdModel