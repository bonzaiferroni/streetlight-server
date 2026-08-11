package streetlight.server.utils

import io.ktor.http.HttpStatusCode
import io.ktor.http.parametersOf
import io.ktor.server.request.host
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext

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

fun Route.subdomain(vararg roots: String, build: Route.() -> Unit): Route =
    createChild(SubdomainSelector(*roots)).apply(build)