package streetlight.server.model

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
}
