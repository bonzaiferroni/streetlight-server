package streetlight.server.db.models

data class SessionToken(
    val id: Int = 0,
    val userId: Int = 0,
    val token: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val issuer: String = "",
)