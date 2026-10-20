package streetlight.server.utils

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.uri
import io.ktor.server.response.respondRedirect
import io.ktor.http.parametersOf
import io.ktor.server.request.host
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext

/** Matches a host one label below one of [roots], and passes that label as the `subdomain` parameter. */
class SubdomainSelector(private vararg val roots: String) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        val host = context.call.request.host()
        val root = roots.firstOrNull { host.endsWith(".$it") } ?: return RouteSelectorEvaluation.Failed
        val label = host.removeSuffix(".$root")
        if (label.isEmpty() || label.contains('.')) return RouteSelectorEvaluation.Failed
        return RouteSelectorEvaluation.Success(
            RouteSelectorEvaluation.qualityConstant,
            parametersOf("subdomain", label)
        )
    }
}

/**
 * Redirects to this request's path and query on the host its subdomain sits under, keeping the port and, through a
 * scheme-relative address, the scheme.
 */
suspend fun ApplicationCall.respondBaseHostRedirect() {
    val host = request.headers[HttpHeaders.Host] ?: request.host()
    respondRedirect("//${host.substringAfter('.')}${request.uri}", permanent = false)
}

/** Routes [build] for hosts one label below one of [roots]. */
fun Route.subdomain(vararg roots: String, build: Route.() -> Unit): Route =
    createChild(SubdomainSelector(*roots)).apply(build)