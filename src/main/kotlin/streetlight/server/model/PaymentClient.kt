package streetlight.server.model

import kotlin.time.Instant
import streetlight.model.data.PaymentStatus
import streetlight.model.data.PaymentId
import streetlight.model.data.CustomerId
import kampfire.api.EmailAddress
import kampfire.model.Url
import streetlight.model.data.CapabilityStatus
import streetlight.model.data.PaymentProvider
import streetlight.model.data.SellerConfiguration
import streetlight.model.data.StarId

/** A payment provider's API, through which sellers are given accounts and onboarded. */
interface PaymentClient {
    val provider: PaymentProvider

    /** Creates a seller account under [configuration] for [starId], prefilled with [country] and [email]. */
    suspend fun createAccount(
        starId: StarId,
        configuration: SellerConfiguration,
        country: String,
        email: EmailAddress?,
    ): ProviderAccount

    suspend fun readAccount(accountId: String): ProviderAccount

    /**
     * Creates a single-use link to the provider's onboarding for [accountId], which returns the seller to
     * [returnUrl] when they leave it and sends them to [refreshUrl] when the link has expired.
     */
    suspend fun createOnboardingLink(accountId: String, returnUrl: String, refreshUrl: String): Url

    /**
     * Creates a customer, [name] at [email], on the seller account [accountId], tagged with [customerId], and returns
     * its id.
     */
    suspend fun createCustomer(accountId: String, customerId: CustomerId, email: EmailAddress, name: String?): String

    /** Sets the name of the customer [providerCustomerId] on the seller account [accountId]. */
    suspend fun updateCustomerName(accountId: String, providerCustomerId: String, name: String)

    /** Creates [invoice] on the seller account [accountId], finalizes it and sends it to its customer. */
    suspend fun sendInvoice(accountId: String, invoice: InvoiceRequest): ProviderInvoice

    suspend fun readInvoice(accountId: String, invoiceId: String): ProviderInvoice
}

/**
 * An invoice of one line, [description] at [amountCents] in [currency], to the provider's customer
 * [providerCustomerId], due [daysUntilDue] after it is sent, withholding [platformFeeCents] for Streetlight.
 */
data class InvoiceRequest(
    val paymentId: PaymentId,
    val providerCustomerId: String,
    val description: String,
    val amountCents: Int,
    val currency: String,
    val platformFeeCents: Int,
    val daysUntilDue: Int,
)

/** The state of an invoice as the provider reports it, with [url] the page its customer pays on. */
data class ProviderInvoice(
    val invoiceId: String,
    val status: PaymentStatus,
    val url: Url?,
    val dueAt: Instant?,
    val paidAt: Instant?,
)

/**
 * The state of a seller's account as the provider reports it. [isAwaitingSeller] marks requirements due now that
 * only the seller can meet, and [statusReason] is the provider's code for a charge status short of active.
 */
data class ProviderAccount(
    val accountId: String,
    val country: String,
    val chargeStatus: CapabilityStatus,
    val payoutStatus: CapabilityStatus,
    val isAwaitingSeller: Boolean,
    val defaultCurrency: String?,
    val statusReason: String?,
)

/** An event a payment provider reports to the webhook. */
sealed interface ProviderEvent {
    val eventId: String

    /** A seller account changed; its new state is read from the provider, since events may arrive out of order. */
    data class AccountUpdated(override val eventId: String, val accountId: String): ProviderEvent

    /** A seller's account was closed, or its seller removed the platform's access to it. */
    data class AccountRemoved(override val eventId: String, val accountId: String): ProviderEvent

    /** An invoice on the seller account [accountId] changed; its new state is read from the provider. */
    data class InvoiceUpdated(override val eventId: String, val accountId: String, val invoiceId: String): ProviderEvent

    /** An event of a [type] that is not handled. */
    data class Unhandled(override val eventId: String, val type: String): ProviderEvent
}
