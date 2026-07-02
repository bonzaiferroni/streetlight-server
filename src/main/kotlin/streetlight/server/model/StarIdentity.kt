package streetlight.server.model

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import kampfire.api.Username
import kampfire.model.Identity
import kampfire.model.Outcome
import kampfire.model.SessionIdentity
import kampfire.model.UserRole
import streetlight.model.data.StarId

data class StarIdentity(
    val starId: StarId,
    val username: Username,
    val roles: Set<UserRole>,
) : Identity

fun RoutingCall.getIdentityOrNull() = principal<SessionIdentity>()?.identity as? StarIdentity
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