package streetlight.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import klutch.db.model.SessionIdentity
import kotlin.time.Duration.Companion.seconds

/** Installs the rate limits of [RateLimits], or unlimited ones when [withLimits] is off. */
fun Application.configureRateLimits(withLimits: Boolean) {
    val limit = if (withLimits) 5 else Int.MAX_VALUE

    install(RateLimit) {
        register(RateLimits.Login) {
            rateLimiter(limit = limit, refillPeriod = 60.seconds)
            requestKey { call -> call.sessionIdOrIp() }
        }
        register(RateLimits.AccountActions) {
            rateLimiter(limit = limit, refillPeriod = 60.seconds)
            requestKey { call -> call.sessionIdOrIp() }
        }
    }
}

object RateLimits {
    val Login = RateLimitName("login")
    val AccountActions = RateLimitName("account-actions")
}

private fun ApplicationCall.sessionIdOrIp() = principal<SessionIdentity>()?.session?.sessionId ?: request.origin.remoteHost