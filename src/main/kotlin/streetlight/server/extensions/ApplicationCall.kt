package streetlight.server.extensions

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import streetlight.server.plugins.CLAIM_ROLES
import streetlight.server.plugins.CLAIM_USERNAME

fun ApplicationCall.getIdOrThrow(): Int {
    return this.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
}

fun ApplicationCall.getClaim(name: String): String {
    return this.principal<JWTPrincipal>()?.payload?.getClaim(name)?.asString() ?: ""
}

fun ApplicationCall.testRole(role: String): Boolean {
    return this.getClaim(CLAIM_ROLES).contains(role)
}

fun ApplicationCall.getUsername(): String {
    return this.getClaim(CLAIM_USERNAME)
}