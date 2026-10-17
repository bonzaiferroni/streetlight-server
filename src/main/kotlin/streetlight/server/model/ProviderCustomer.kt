package streetlight.server.model

import kampfire.api.EmailAddress
import streetlight.model.data.CustomerId
import streetlight.model.data.PaymentProvider
import streetlight.model.data.StarId

/** A buyer of [starId]'s, known to the provider as [providerCustomerId] on the seller's account [providerAccountId]. */
data class ProviderCustomer(
    val customerId: CustomerId,
    val starId: StarId,
    val provider: PaymentProvider,
    val providerAccountId: String,
    val providerCustomerId: String,
    val email: EmailAddress,
    val name: String?,
)
