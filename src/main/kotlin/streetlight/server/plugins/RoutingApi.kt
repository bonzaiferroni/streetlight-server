package streetlight.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import streetlight.model.User
import streetlight.server.db.applyServiceRouting
import streetlight.server.db.services.AreaService
import streetlight.server.db.services.EventInfoService
import streetlight.server.db.services.EventService
import streetlight.server.db.services.RequestService
import streetlight.server.db.services.eventInfoRouting
import streetlight.server.db.services.LocationService
import streetlight.server.db.services.RequestInfoService
import streetlight.server.db.services.requestInfoRouting
import streetlight.server.db.services.SongService
import streetlight.server.db.services.UserService
import streetlight.server.db.services.eventRouting
import java.util.Date

fun Application.configureApiRoutes() {
    routing {
        get(v1) {
            call.respondText("Hello World!")
        }

        applyServiceRouting(AreaService())
        applyServiceRouting(EventService())
        applyServiceRouting(UserService())
        applyServiceRouting(LocationService())
        applyServiceRouting(RequestService())
        applyServiceRouting(SongService())
        applyServiceRouting(RequestService())

        eventInfoRouting(EventInfoService())
        requestInfoRouting(RequestInfoService())
        eventRouting(EventService())

        post("$v1/login") {
            val audience = "http://localhost:8080/"
            val issuer = "http://localhost:8080/"
            val secret = "secret"

            val user = call.receive<User>()
            if (user.name != "admin" || user.password != "admin") {
                call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                return@post
            }

            val token = JWT.create()
                .withAudience(audience)
                .withIssuer(issuer)
                .withExpiresAt(Date(System.currentTimeMillis() + 60000))
                // .withClaim("username", user.name)
                .sign(Algorithm.HMAC256(secret))
            call.respond(hashMapOf("token" to token))
        }
    }
}

val v1 = "/api/v1"