package streetlight.server.plugins

import streetlight.server.EnvKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import kabinet.utils.Environment
import streetlight.server.service.applyProviderEvent
import streetlight.server.service.recordBounce
import streetlight.server.external.PostmarkBounce
import streetlight.server.external.isValidStripeSignature
import streetlight.server.external.readStripeEvent
import streetlight.server.model.ApiScope
import kotlin.time.Clock

/**
 * Receives Postmark's bounce reports at a path holding a secret, recording each address it marks inactive, and
 * Stripe's events on connected accounts, verified by their signature.
 */
fun ApiScope.serveWebhooks() {
    val postmarkWebhookSecret = provide(Environment::class).read(EnvKey.POSTMARK_WEBHOOK_SECRET)
    post("/webhooks/postmark/{secret}") {
        if (call.parameters["secret"] != postmarkWebhookSecret) {
            call.respond(HttpStatusCode.NotFound)
            return@post
        }

        val payload = try {
            call.receive<PostmarkBounce>()
        } catch (e: Exception) {
            log.error(e) { "malformed postmark webhook payload" }
            call.respond(HttpStatusCode.OK)
            return@post
        }

        call.respond(HttpStatusCode.OK)

        if (!payload.inactive) return@post

        recordBounce(payload)
    }

    post("/webhooks/stripe/connect") {
        val payload = call.receiveText()
        val secret = provide(Environment::class).read(EnvKey.STRIPE_CONNECT_WEBHOOK_SECRET)
        val signature = call.request.header("Stripe-Signature")
        if (!isValidStripeSignature(payload, signature, secret, Clock.System.now())) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        applyProviderEvent(readStripeEvent(payload))
        call.respond(HttpStatusCode.OK)
    }
}
