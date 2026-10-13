package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.CapabilityStatus
import streetlight.model.data.PaymentProvider
import streetlight.model.data.Seller
import streetlight.model.data.SellerConfiguration
import streetlight.model.data.StarId
import streetlight.server.model.ProviderAccount
import kotlin.time.Instant

/** The registration of each selling star with a payment provider. */
object SellerTable: Table("seller") {
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val provider = enumeration<PaymentProvider>("provider")
    val providerAccountId = text("provider_account_id")
    val configuration = enumeration<SellerConfiguration>("configuration")
    val country = text("country")
    val defaultCurrency = text("default_currency").nullable()
    val chargeStatus = enumeration<CapabilityStatus>("charge_status")
    val payoutStatus = enumeration<CapabilityStatus>("payout_status")
    val isAwaitingSeller = bool("is_awaiting_seller")
    val statusReason = text("status_reason").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(starId, provider)

    init {
        uniqueIndex(provider, providerAccountId)
    }
}

fun ResultRow.toSeller() = Seller(
    starId = StarId(this[SellerTable.starId].value),
    provider = this[SellerTable.provider],
    providerAccountId = this[SellerTable.providerAccountId],
    configuration = this[SellerTable.configuration],
    country = this[SellerTable.country],
    chargeStatus = this[SellerTable.chargeStatus],
    payoutStatus = this[SellerTable.payoutStatus],
    isAwaitingSeller = this[SellerTable.isAwaitingSeller],
    defaultCurrency = this[SellerTable.defaultCurrency],
    statusReason = this[SellerTable.statusReason],
    createdAt = this[SellerTable.createdAt],
    updatedAt = this[SellerTable.updatedAt],
)

fun UpdateBuilder<*>.createSeller(
    starId: StarId,
    provider: PaymentProvider,
    configuration: SellerConfiguration,
    account: ProviderAccount,
    now: Instant,
) {
    this[SellerTable.starId] = starId.value
    this[SellerTable.provider] = provider
    this[SellerTable.providerAccountId] = account.accountId
    this[SellerTable.configuration] = configuration
    this[SellerTable.country] = account.country
    this[SellerTable.createdAt] = now
    updateSeller(account, now)
}

fun UpdateBuilder<*>.updateSeller(account: ProviderAccount, now: Instant) {
    this[SellerTable.defaultCurrency] = account.defaultCurrency
    this[SellerTable.chargeStatus] = account.chargeStatus
    this[SellerTable.payoutStatus] = account.payoutStatus
    this[SellerTable.isAwaitingSeller] = account.isAwaitingSeller
    this[SellerTable.statusReason] = account.statusReason
    this[SellerTable.updatedAt] = now
}
