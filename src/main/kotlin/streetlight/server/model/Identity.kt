package streetlight.server.model

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import kampfire.api.Username
import kampfire.model.Outcome
import kampfire.model.UserRole
import klutch.db.model.Identity
import klutch.db.model.SessionIdentity
import streetlight.model.data.StarId
import kotlin.uuid.Uuid

fun RoutingCall.getIdentityOrNull() = principal<SessionIdentity>()?.identity
fun RoutingCall.getIdentity() = getIdentityOrNull() ?: error("identity not found")

@Deprecated("this should not be necessary")
suspend fun <T> RoutingContext.requireIdentity(block: suspend (Identity) -> Outcome<T>?): Outcome<T>? =
    when (val identity = call.getIdentityOrNull()) {
        null -> {
            call.respond(HttpStatusCode.Unauthorized)
            null
        }

        else -> block(identity)
    }