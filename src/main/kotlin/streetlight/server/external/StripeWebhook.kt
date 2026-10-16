package streetlight.server.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import streetlight.server.model.ProviderEvent
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Whether [header], the `Stripe-Signature` of a webhook request, signs [payload] with [secret] at a time within
 * [tolerance] of [now].
 */
fun isValidStripeSignature(
    payload: String,
    header: String?,
    secret: String,
    now: Instant,
    tolerance: Duration = 5.minutes,
): Boolean {
    if (header == null) return false
    val parts = header.split(',').mapNotNull { part ->
        part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() }
    }
    val timestamp = parts.firstOrNull { it.first == "t" }?.second?.toLongOrNull() ?: return false
    val signedAt = Instant.fromEpochSeconds(timestamp)
    if (now - signedAt > tolerance || signedAt - now > tolerance) return false

    val expected = signStripePayload(payload, timestamp, secret)
    return parts.filter { it.first == "v1" }.any { (_, signature) ->
        MessageDigest.isEqual(expected.toByteArray(), signature.toByteArray())
    }
}

/** The hex HMAC-SHA256 of [payload] signed at [timestamp] with [secret], as Stripe computes it. */
fun signStripePayload(payload: String, timestamp: Long, secret: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
    return mac.doFinal("$timestamp.$payload".toByteArray()).toHexString()
}

/** Reads a Stripe thin event [payload] into the event it reports. */
fun readStripeEvent(payload: String): ProviderEvent {
    val event = stripeJson.decodeFromString<StripeThinEvent>(payload)
    val accountId = event.context ?: event.relatedObject?.id
    return when (event.type) {
        in accountUpdateTypes -> ProviderEvent.AccountUpdated(
            eventId = event.id,
            accountId = event.relatedObject?.id ?: error("account event ${event.id} names no account")
        )
        "v2.core.account.closed", "v1.account.application.deauthorized" -> ProviderEvent.AccountRemoved(
            eventId = event.id,
            accountId = accountId ?: error("removal event ${event.id} names no account")
        )
        in invoiceUpdateTypes -> ProviderEvent.InvoiceUpdated(
            eventId = event.id,
            accountId = event.context ?: error("invoice event ${event.id} names no account"),
            invoiceId = event.relatedObject?.id ?: error("invoice event ${event.id} names no invoice"),
        )
        else -> ProviderEvent.Unhandled(event.id, event.type)
    }
}

/** The thin event types after which a seller's account is read again. */
val accountUpdateTypes = setOf(
    "v2.core.account.updated",
    "v2.core.account[requirements].updated",
    "v2.core.account[configuration.merchant].updated",
    "v2.core.account[configuration.merchant].capability_status_updated",
)

/** The thin event types after which a payment's invoice is read again. */
val invoiceUpdateTypes = setOf(
    "v1.invoice.paid",
    "v1.invoice.voided",
    "v1.invoice.marked_uncollectible",
)

/** A thin event: what happened and the object it happened to, without the object's state. */
@Serializable
data class StripeThinEvent(
    val id: String,
    val type: String,
    val context: String? = null,
    @SerialName("related_object") val relatedObject: StripeRelatedObject? = null,
)

@Serializable
data class StripeRelatedObject(
    val id: String,
    val type: String,
)
