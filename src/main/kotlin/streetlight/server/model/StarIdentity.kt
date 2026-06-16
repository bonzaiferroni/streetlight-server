package streetlight.server.model

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import kampfire.api.Username
import kampfire.model.ApiResponse
import kampfire.model.UserRole
import streetlight.model.data.StarId

data class StarIdentity(
    val starId: StarId,
    val username: Username,
    val roles: Set<UserRole>
)

fun RoutingCall.getIdentityOrNull() = principal<StarIdentity>()
fun RoutingCall.getIdentity() = principal<StarIdentity>() ?: error("identity not found")

suspend fun <T> RoutingContext.requireIdentity(
    block: suspend RoutingContext.(StarIdentity) -> ApiResponse<T>?
): ApiResponse<T>? {
    val identity = call.getIdentityOrNull()
    return if (identity != null) {
        block(identity)
    } else {
        call.respond(HttpStatusCode.Unauthorized)
        null
    }
}