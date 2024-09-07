package streetlight.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import streetlight.server.db.core.VariableStore

fun Application.configureSecurity() {
    // Please read the jwt property from the config file if you are using EngineMain
    val audience = "http://localhost:8080/"
    val issuer = "http://localhost:8080/"
    val jwtRealm = "streetlight api"
    val jwtSecret = VariableStore().appSecret
    authentication {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                println(credential.payload.audience)
                if (credential.payload.audience.contains(audience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
