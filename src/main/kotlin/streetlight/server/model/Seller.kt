package streetlight.server.model

import kampfire.api.TableId
import streetlight.model.data.CapabilityStatus
import streetlight.model.data.PaymentProvider
import streetlight.model.data.SellerConfiguration
import streetlight.model.data.SellerStatus
import streetlight.model.data.StarId
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** A user's registration with a [PaymentProvider], through which they accept payments for offerings. */
data class Seller(
    val sellerId: SellerId,
    val starId: StarId,
    val provider: PaymentProvider,
    val providerAccountId: String,
    val configuration: SellerConfiguration,
    val country: String,
    val chargeStatus: CapabilityStatus,
    val payoutStatus: CapabilityStatus,
    val isAwaitingSeller: Boolean,
    val defaultCurrency: String?,
    val statusReason: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@JvmInline
value class SellerId(override val value: Uuid): TableId<Uuid> {
    override fun toString() = value.toString()
}

fun Seller.toSellerStatus() = SellerStatus(
    chargeStatus = chargeStatus,
    payoutStatus = payoutStatus,
    isAwaitingSeller = isAwaitingSeller,
    statusReason = statusReason,
)
