package streetlight.server.external

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kabinet.utils.Environment
import kampfire.api.EmailAddress
import kampfire.model.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import streetlight.model.data.CapabilityStatus
import streetlight.model.data.CustomerId
import streetlight.model.data.PaymentStatus
import streetlight.model.data.PaymentProvider
import streetlight.model.data.SellerConfiguration
import streetlight.model.data.StarId
import streetlight.server.EnvKey
import streetlight.server.model.InvoiceRequest
import streetlight.server.model.PaymentClient
import streetlight.server.model.ProviderInvoice
import streetlight.server.model.ProviderAccount
import kotlin.time.Instant

/**
 * Creates and onboards Stripe connected accounts through the Accounts v2 API, and invoices their customers through
 * the v1 API as the connected account.
 */
class StripePaymentClient(env: Environment): PaymentClient {
    private val secretKey by lazy { env.read(EnvKey.STRIPE_SECRET_KEY) }

    override val provider = PaymentProvider.Stripe

    private val client = HttpClient(CIO) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(stripeJson)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 5_000
        }

        defaultRequest {
            url("https://api.stripe.com/v2/core/")
            bearerAuth(secretKey)
            header("Stripe-Version", STRIPE_API_VERSION)
        }
    }

    override suspend fun createAccount(
        starId: StarId,
        configuration: SellerConfiguration,
        country: String,
        email: EmailAddress?,
    ): ProviderAccount {
        val request = when (configuration) {
            SellerConfiguration.Standard -> StripeAccountCreate(
                contactEmail = email?.value,
                dashboard = "full",
                identity = StripeIdentity(country = country.lowercase()),
                configuration = StripeConfigurationRequest(
                    merchant = StripeMerchantRequest(
                        capabilities = StripeMerchantCapabilitiesRequest(cardPayments = StripeRequested(true))
                    )
                ),
                defaults = StripeDefaults(
                    responsibilities = StripeResponsibilities(feesCollector = "stripe", lossesCollector = "stripe")
                ),
                metadata = mapOf("starId" to starId.value.toString()),
                include = accountIncludes,
            )
        }
        val response = client.post("accounts") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.readStripe<StripeAccount>().toProviderAccount()
    }

    override suspend fun readAccount(accountId: String): ProviderAccount {
        val response = client.get("accounts/$accountId") {
            accountIncludes.forEachIndexed { index, include -> parameter("include[$index]", include) }
        }
        return response.readStripe<StripeAccount>().toProviderAccount()
    }

    override suspend fun createOnboardingLink(accountId: String, returnUrl: String, refreshUrl: String): Url {
        val request = StripeAccountLinkCreate(
            account = accountId,
            useCase = StripeLinkUseCase(
                type = "account_onboarding",
                accountOnboarding = StripeAccountOnboarding(
                    returnUrl = returnUrl,
                    refreshUrl = refreshUrl,
                )
            )
        )
        val response = client.post("account_links") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return Url(response.readStripe<StripeAccountLink>().url)
    }

    override suspend fun createCustomer(
        accountId: String,
        customerId: CustomerId,
        email: EmailAddress,
        name: String?,
    ): String {
        val response = postV1(accountId, "customers", "customer-$customerId") {
            append("email", email.value)
            name?.let { append("name", it) }
            append("metadata[customerId]", customerId.toString())
        }
        return response.readStripe<StripeCustomer>().id
    }

    override suspend fun updateCustomerName(accountId: String, providerCustomerId: String, name: String) {
        postV1(accountId, "customers/$providerCustomerId", "customer-name-$providerCustomerId-$name") {
            append("name", name)
        }.readStripe<StripeCustomer>()
    }

    override suspend fun sendInvoice(accountId: String, invoice: InvoiceRequest): ProviderInvoice {
        val paymentId = invoice.paymentId.toString()
        val draft = postV1(accountId, "invoices", "invoice-$paymentId") {
            append("customer", invoice.providerCustomerId)
            append("collection_method", "send_invoice")
            append("days_until_due", invoice.daysUntilDue.toString())
            append("currency", invoice.currency)
            append("pending_invoice_items_behavior", "exclude")
            append("metadata[paymentId]", paymentId)
            if (invoice.platformFeeCents > 0) append("application_fee_amount", invoice.platformFeeCents.toString())
        }.readStripe<StripeInvoice>()
        postV1(accountId, "invoiceitems", "invoice-item-$paymentId") {
            append("customer", invoice.providerCustomerId)
            append("invoice", draft.id)
            append("amount", invoice.amountCents.toString())
            append("currency", invoice.currency)
            append("description", invoice.description)
        }.readStripe<StripeInvoiceItem>()
        val response = postV1(accountId, "invoices/${draft.id}/send", "invoice-send-$paymentId") { }
        return response.readStripe<StripeInvoice>().toProviderInvoice()
    }

    override suspend fun readInvoice(accountId: String, invoiceId: String): ProviderInvoice {
        val response = client.get("$STRIPE_V1_URL/invoices/$invoiceId") {
            header("Stripe-Account", accountId)
        }
        return response.readStripe<StripeInvoice>().toProviderInvoice()
    }

    /** Posts the form [build] makes to the v1 [path], as the seller account [accountId], once for [idempotencyKey]. */
    private suspend fun postV1(
        accountId: String,
        path: String,
        idempotencyKey: String,
        build: ParametersBuilder.() -> Unit,
    ) = client.submitForm("$STRIPE_V1_URL/$path", Parameters.build(build)) {
        header("Stripe-Account", accountId)
        header("Idempotency-Key", idempotencyKey)
    }
}

private const val STRIPE_V1_URL = "https://api.stripe.com/v1"

/** The Stripe API version every request is pinned to. */
const val STRIPE_API_VERSION = "2026-09-30.endive"

private val accountIncludes = listOf("configuration.merchant", "identity", "defaults", "requirements")

private suspend inline fun <reified T> HttpResponse.readStripe(): T {
    if (!status.isSuccess()) error("Stripe request failed with $status: ${bodyAsText()}")
    return body()
}

internal val stripeJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

@Serializable
data class StripeAccountCreate(
    @SerialName("contact_email") val contactEmail: String?,
    val dashboard: String,
    val identity: StripeIdentity,
    val configuration: StripeConfigurationRequest,
    val defaults: StripeDefaults,
    val metadata: Map<String, String>,
    val include: List<String>,
)

@Serializable
data class StripeIdentity(
    val country: String,
)

@Serializable
data class StripeConfigurationRequest(
    val merchant: StripeMerchantRequest,
)

@Serializable
data class StripeMerchantRequest(
    val capabilities: StripeMerchantCapabilitiesRequest,
)

@Serializable
data class StripeMerchantCapabilitiesRequest(
    @SerialName("card_payments") val cardPayments: StripeRequested,
)

@Serializable
data class StripeRequested(
    val requested: Boolean,
)

@Serializable
data class StripeDefaults(
    val currency: String? = null,
    val responsibilities: StripeResponsibilities? = null,
)

@Serializable
data class StripeResponsibilities(
    @SerialName("fees_collector") val feesCollector: String,
    @SerialName("losses_collector") val lossesCollector: String,
)

@Serializable
data class StripeAccountLinkCreate(
    val account: String,
    @SerialName("use_case") val useCase: StripeLinkUseCase,
)

@Serializable
data class StripeLinkUseCase(
    val type: String,
    @SerialName("account_onboarding") val accountOnboarding: StripeAccountOnboarding,
)

@Serializable
data class StripeAccountOnboarding(
    @SerialName("return_url") val returnUrl: String,
    @SerialName("refresh_url") val refreshUrl: String,
)

@Serializable
data class StripeAccountLink(
    val url: String,
)

/** The fields of a v2 Stripe account that a seller's flags are read from. */
@Serializable
data class StripeAccount(
    val id: String,
    val identity: StripeIdentity? = null,
    val defaults: StripeDefaults? = null,
    val configuration: StripeConfiguration? = null,
    val requirements: StripeRequirements? = null,
)

@Serializable
data class StripeConfiguration(
    val merchant: StripeMerchant? = null,
)

@Serializable
data class StripeMerchant(
    val capabilities: StripeMerchantCapabilities? = null,
)

@Serializable
data class StripeMerchantCapabilities(
    @SerialName("card_payments") val cardPayments: StripeCapability? = null,
    @SerialName("stripe_balance") val stripeBalance: StripeBalanceCapabilities? = null,
)

@Serializable
data class StripeBalanceCapabilities(
    val payouts: StripeCapability? = null,
)

@Serializable
data class StripeCapability(
    val status: String,
    @SerialName("status_details") val statusDetails: List<StripeStatusDetail> = emptyList(),
)

@Serializable
data class StripeStatusDetail(
    val code: String,
)

@Serializable
data class StripeRequirements(
    val entries: List<StripeRequirement>? = null,
)

@Serializable
data class StripeRequirement(
    @SerialName("awaiting_action_from") val awaitingActionFrom: String,
    @SerialName("minimum_deadline") val minimumDeadline: StripeDeadline,
)

@Serializable
data class StripeDeadline(
    val status: String,
)

/**
 * Reads the account into the provider-neutral state. A missing or unknown capability status reads as restricted.
 *
 * Stripe restricts a capability while it verifies details, so a restriction whose due requirements all await Stripe
 * reads as pending.
 */
fun StripeAccount.toProviderAccount(): ProviderAccount {
    val dueEntries = requirements?.entries.orEmpty().filter { it.minimumDeadline.status != "eventually_due" }
    val isAwaitingSeller = dueEntries.any { it.awaitingActionFrom == "user" }
    val isUnderReview = !isAwaitingSeller && dueEntries.any { it.awaitingActionFrom == "stripe" }
    val capabilities = configuration?.merchant?.capabilities
    val cardPayments = capabilities?.cardPayments
    val chargeStatus = cardPayments.toCapabilityStatus(isUnderReview)
    return ProviderAccount(
        accountId = id,
        country = identity?.country?.uppercase() ?: error("Stripe account $id has no country"),
        chargeStatus = chargeStatus,
        payoutStatus = capabilities?.stripeBalance?.payouts.toCapabilityStatus(isUnderReview),
        isAwaitingSeller = isAwaitingSeller,
        defaultCurrency = defaults?.currency,
        statusReason = cardPayments?.takeIf { chargeStatus == CapabilityStatus.Restricted }
            ?.statusDetails?.firstOrNull()?.code,
    )
}

private fun StripeCapability?.toCapabilityStatus(isUnderReview: Boolean) = when (this?.status) {
    "active" -> CapabilityStatus.Active
    "pending" -> CapabilityStatus.Pending
    "unsupported" -> CapabilityStatus.Unsupported
    "restricted" if isUnderReview -> CapabilityStatus.Pending
    else -> CapabilityStatus.Restricted
}

@Serializable
data class StripeCustomer(
    val id: String,
)

@Serializable
data class StripeInvoiceItem(
    val id: String,
)

/** The fields of a v1 Stripe invoice that a payment's state is read from. */
@Serializable
data class StripeInvoice(
    val id: String,
    val status: String,
    @SerialName("hosted_invoice_url") val hostedInvoiceUrl: String? = null,
    @SerialName("due_date") val dueDate: Long? = null,
    @SerialName("status_transitions") val statusTransitions: StripeStatusTransitions? = null,
)

@Serializable
data class StripeStatusTransitions(
    @SerialName("paid_at") val paidAt: Long? = null,
)

fun StripeInvoice.toProviderInvoice() = ProviderInvoice(
    invoiceId = id,
    status = when (status) {
        "draft" -> PaymentStatus.Draft
        "open" -> PaymentStatus.Open
        "paid" -> PaymentStatus.Paid
        "void" -> PaymentStatus.Void
        "uncollectible" -> PaymentStatus.Uncollectible
        else -> error("Stripe invoice $id has unknown status $status")
    },
    url = hostedInvoiceUrl?.let { Url(it) },
    dueAt = dueDate?.let { Instant.fromEpochSeconds(it) },
    paidAt = statusTransitions?.paidAt?.let { Instant.fromEpochSeconds(it) },
)
