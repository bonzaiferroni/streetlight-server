package streetlight.server.model

import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingCall
import kampfire.api.Username
import kampfire.model.HashedToken
import kampfire.model.Principal
import kampfire.model.UserRole
import streetlight.model.data.StarId
import kotlin.time.Instant

data class StarIdentity(
    val starId: StarId,
    val username: Username,
    val roles: Set<UserRole>,
    val token: HashedToken,
): Principal

fun RoutingCall.getIdentityOrNull() = principal<StarIdentity>()
fun RoutingCall.getIdentity() = principal<StarIdentity>() ?: error("identity not found")
