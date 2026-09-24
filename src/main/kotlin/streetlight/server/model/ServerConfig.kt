package streetlight.server.model

/** The optional parts of the server, all on by default; tests turn off what they do not need. */
data class ServerConfig(
    val withMetrics: Boolean = true,
    val withDatabase: Boolean = true,
    val withTransit: Boolean = true,
    val withRateLimits: Boolean = true,
)
