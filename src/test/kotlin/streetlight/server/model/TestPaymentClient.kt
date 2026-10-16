package streetlight.server.model

import streetlight.model.data.PaymentStatus
import streetlight.model.data.CustomerId
import kampfire.api.EmailAddress
import kampfire.model.Url
import streetlight.model.data.CapabilityStatus
import streetlight.model.data.PaymentProvider
import streetlight.model.data.SellerConfiguration
import streetlight.model.data.StarId

/** A payment provider holding its accounts in memory, each created with no requirements met. */
class TestPaymentClient: PaymentClient {
    override val provider = PaymentProvider.Stripe

    /** The accounts by id; a test sets one to change what the provider reports. */
    val accounts = mutableMapOf<String, ProviderAccount>()

    /** The customer ids by email, across accounts. */
    val customers = mutableMapOf<EmailAddress, String>()

    /** The invoices by id; a test sets one to change what the provider reports. */
    val invoices = mutableMapOf<String, ProviderInvoice>()

    override suspend fun createAccount(
        starId: StarId,
        configuration: SellerConfiguration,
        country: String,
        email: EmailAddress?,
    ): ProviderAccount {
        val account = ProviderAccount(
            accountId = "acct_test_${accounts.size + 1}",
            country = country,
            chargeStatus = CapabilityStatus.Restricted,
            payoutStatus = CapabilityStatus.Restricted,
            isAwaitingSeller = true,
            defaultCurrency = "usd",
            statusReason = "requirements_past_due",
        )
        accounts[account.accountId] = account
        return account
    }

    override suspend fun readAccount(accountId: String) =
        accounts[accountId] ?: error("no test account with id $accountId")

    override suspend fun createOnboardingLink(accountId: String, returnUrl: String, refreshUrl: String): Url {
        readAccount(accountId)
        return Url("https://connect.stripe.test/setup/$accountId")
    }

    override suspend fun createCustomer(accountId: String, customerId: CustomerId, email: EmailAddress): String {
        readAccount(accountId)
        return customers.getOrPut(email) { "cus_test_${customers.size + 1}" }
    }

    override suspend fun sendInvoice(accountId: String, invoice: InvoiceRequest): ProviderInvoice {
        readAccount(accountId)
        val invoiceId = "in_test_${invoices.size + 1}"
        val sent = ProviderInvoice(
            invoiceId = invoiceId,
            status = PaymentStatus.Open,
            url = Url("https://invoice.stripe.test/$invoiceId"),
            dueAt = null,
            paidAt = null,
        )
        invoices[invoiceId] = sent
        return sent
    }

    override suspend fun readInvoice(accountId: String, invoiceId: String) =
        invoices[invoiceId] ?: error("no test invoice with id $invoiceId")
}
