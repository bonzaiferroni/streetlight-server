package streetlight.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import streetlight.server.db.core.VariableStore

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
                if (credential.payload.audience.contains(audience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}

const val TOKEN_NAME = "auth-jwt"

fun Route.authenticateJwt(block: Route.() -> Unit) {
    authenticate(TOKEN_NAME) {
        block()
    }
}