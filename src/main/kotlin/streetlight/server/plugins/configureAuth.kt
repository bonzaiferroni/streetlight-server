package streetlight.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kampfire.api.StringId
import kampfire.model.Token
import kampfire.model.UserRole
import kampfire.model.toClaimValue
import klutch.environment.readEnvFromPath
import klutch.utils.serverLog
import streetlight.model.data.StarId
import streetlight.server.model.StreetlightServer
import java.security.SecureRandom
import java.util.Base64
import java.util.Date

fun Application.configureAuth(server: StreetlightServer) {
    val secret = env.read(APP_SECRET_KEY)
    authentication {
        jwt(TOKEN_NAME) {
            realm = TokenProperty.Realm
            authHeader { call ->
                call.request.cookies["auth_token"]?.let {
                    HttpAuthHeader.Single("Bearer", it)
                }
            }
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(TokenProperty.Audience)
                    .withIssuer(TokenProperty.Issuer)
                    .withClaimPresence("sub")
                    .build()
            )
            validate { credential ->
                credential.payload.subject?.let {
                    server.dao.star.readStarPrincipal(StarId(it))
                }
            }
            challenge { _, _ ->
                serverLog.logDebug("Security: JWT authentication failed")
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}

object TokenProperty {
    const val Audience = "streetlight-api"
    const val Issuer = "streetlight-auth"
    const val Realm = "streetlight-api"
    const val LifetimeSeconds = 10 // 30 * 60 // 30 minutes
}

const val TOKEN_NAME = "auth-jwt"
const val APP_SECRET_KEY = "APP_SECRET"

private val env = readEnvFromPath()

fun createAccessToken(userId: StringId): Token {
    val secret = env.read(APP_SECRET_KEY)
    val value = JWT.create()
        .withAudience(TokenProperty.Audience)
        .withIssuer(TokenProperty.Issuer)
        .withExpiresAt(Date(System.currentTimeMillis() + TokenProperty.LifetimeSeconds * 1000)) // 30 minutes
        .withSubject(userId)
        .sign(Algorithm.HMAC256(secret))
    return Token(value, TokenProperty.LifetimeSeconds)
}

fun Route.authGate(optional: Boolean = false, block: Route.() -> Unit) = authenticate(TOKEN_NAME, optional = optional) {
    block()
}

private fun generateSecret(): String {
    val bytes = ByteArray(64)
    SecureRandom().nextBytes(bytes)
    return Base64.getEncoder().encodeToString(bytes)
}