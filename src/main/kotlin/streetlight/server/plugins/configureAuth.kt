package streetlight.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kampfire.api.StringId
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
    val secret = env.read("APP_SECRET")
    authentication {
        jwt(TOKEN_NAME) {
            realm = TokenProperty.Realm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(TokenProperty.Audience)
                    .withIssuer(TokenProperty.Issuer)
                    .withClaimPresence("sub")
                    .build()
            )
            validate { credential ->
                val userIdExists = credential.payload.subject?.let {
                    server.dao.star.readUserIdExists(StarId(it))
                } ?: false
                if (userIdExists) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                serverLog.logDebug("Security: JWT authentication failed")
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}

object TokenClaim {
    const val Username = "username"
    const val Roles = "roles"
}

object TokenProperty {
    const val Audience = "streetlight-api"
    const val Issuer = "streetlight-auth"
    const val Realm = "streetlight-api"
}

const val TOKEN_NAME = "auth-jwt"

private val env = readEnvFromPath()

fun createAccessToken(userId: StringId, username: String, roles: Set<UserRole>): String {
    val secret = env.read("APP_SECRET")
    return JWT.create()
        .withAudience(TokenProperty.Audience)
        .withIssuer(TokenProperty.Issuer)
        .withExpiresAt(Date(System.currentTimeMillis() + 60000 * 30)) // 30 minutes
        .withSubject(userId)
        .withClaim(TokenClaim.Username, username)
        .withClaim(TokenClaim.Roles, roles.toClaimValue())
        .sign(Algorithm.HMAC256(secret))
}

fun Route.authGate(optional: Boolean = false, block: Route.() -> Unit) = authenticate(TOKEN_NAME, optional = optional) {
    block()
}

private fun generateSecret(): String {
    val bytes = ByteArray(64)
    SecureRandom().nextBytes(bytes)
    return Base64.getEncoder().encodeToString(bytes)
}