package streetlight.server.model

import io.ktor.server.routing.Routing

class RoutingContext<out T>(
    val model: T,
    context: Routing
): Routing by context {
}

fun <T> Routing.routingContextOf(model: T, block: RoutingContext<T>.() -> Unit) = RoutingContext(model, this).block()

