package streetlight.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import kabinet.utils.Environment
import streetlight.server.db.datascope.recordBounce
import streetlight.server.external.PostmarkBounce
import streetlight.server.model.ApiScope

/** Receives Postmark's bounce reports at a path holding a secret, recording each address it marks inactive. */
fun ApiScope.serveWebhooks() {
    val postmarkWebhookSecret = provide(Environment::class).read("POSTMARK_WEBHOOK_SECRET")
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
}