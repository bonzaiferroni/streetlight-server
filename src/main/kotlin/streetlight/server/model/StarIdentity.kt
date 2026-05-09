package streetlight.server.model

import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingCall
import kampfire.model.UserRole
import streetlight.model.data.StarId

data class StarIdentity(
    val starId: StarId,
    val username: String,
    val roles: Set<UserRole>
)

fun RoutingCall.getIdentityOrNull() = principal<StarIdentity>()
fun RoutingCall.getIdentity() = principal<StarIdentity>() ?: error("identity not found")