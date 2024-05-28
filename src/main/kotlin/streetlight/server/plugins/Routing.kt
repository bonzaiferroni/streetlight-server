package streetlight.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.model.User

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        post("/login") {
            val audience = "jwt-audience"
            val jwtDomain = "https://jwt-provider-domain/"
            val jwtRealm = "streetlight app"
            val secret = "secret"

            val user = call.receive<User>()
            if (user.name != "admin" || user.password != "admin") {
                call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                return@post
            }

            val token = JWT.create()
                .withAudience(audience)
                .withIssuer(jwtDomain)
                .withClaim("username", user.name)
                .sign(Algorithm.HMAC256(secret))
            call.respond(hashMapOf("token" to token))
        }
    }
}
