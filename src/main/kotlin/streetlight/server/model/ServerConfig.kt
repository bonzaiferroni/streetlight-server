package streetlight.server.model

data class ServerConfig(
    val withMetrics: Boolean = true,
    val withDatabase: Boolean = true,
    val withTransit: Boolean = true,
    val withRateLimits: Boolean = true,
)
