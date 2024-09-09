package streetlight.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.server.db.core.VariableStore
import streetlight.server.logError
import streetlight.server.logInfo
import java.util.*

fun Application.configureSecurity() {
    // Please read the jwt property from the config file if you are using EngineMain
    val audience = "http://localhost:8080/"
    val issuer = "http://localhost:8080/"
    val jwtRealm = "streetlight api"
    val jwtSecret = VariableStore().appSecret
    authentication {
        jwt(TOKEN_NAME) {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                println("validate: ${credential.payload}")
                if (credential.payload.audience.contains(audience)) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                // Extract token from header manually
                val authHeader = call.request.headers["Authorization"]
                val token = authHeader?.removePrefix("Bearer ")

                if (token != null) {
                    // Try to decode and log the token contents
                    try {
                        val decodedJWT: DecodedJWT = JWT.decode(token)
                        // call.logError("Token rejected: ${decodedJWT.payload}")
                        // call.logError("Issuer: ${decodedJWT.issuer}")
                        // call.logError("Audience: ${decodedJWT.audience}")
                        // call.logError("Expires At: ${decodedJWT.expiresAt}")
                    } catch (e: JWTVerificationException) {
                        call.logError("Security: Invalid JWT format: ${e.message}")
                    }
                } else {
                    call.logError("Security: No Bearer token found in the request.")
                }
                call.logInfo("Security: JWT authentication failed")
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}

const val TOKEN_NAME = "auth-jwt"
const val CLAIM_USERNAME = "username"
const val CLAIM_ROLES = "roles"
const val ROLE_USER = "user"
const val ROLE_ADMIN = "admin"

fun createJWT(username: String, roles: String): String {
    val audience = "http://localhost:8080/"
    val issuer = "http://localhost:8080/"
    val secret = VariableStore().appSecret
    return JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withExpiresAt(Date(System.currentTimeMillis() + 60000))
        .withClaim(CLAIM_USERNAME, username)
        .withClaim(CLAIM_ROLES, roles)
        .sign(Algorithm.HMAC256(secret))
}

fun Route.authenticateJwt(block: Route.() -> Unit) {
    authenticate(TOKEN_NAME) {
        block()
    }
}

fun ApplicationCall.getClaim(name: String): String {
    return this.principal<JWTPrincipal>()?.payload?.getClaim(name)?.asString() ?: ""
}

fun ApplicationCall.testRole(role: String): Boolean {
    return this.getClaim(CLAIM_ROLES).contains(role)
}