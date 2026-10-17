package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import kampfire.api.toEmailAddress
import streetlight.model.data.CustomerId
import streetlight.model.data.FulfillmentStatus
import streetlight.model.data.MAX_PLATFORM_CUT
import streetlight.model.data.Offering
import streetlight.model.data.OfferingId
import streetlight.model.data.Payment
import streetlight.model.data.PaymentId
import streetlight.model.data.PaymentProvider
import streetlight.model.data.PaymentStatus
import streetlight.server.model.ProviderCustomer
import streetlight.server.model.ProviderInvoice
import streetlight.server.model.Seller
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Each payment a seller requests for an offering, with the price, fee and account it was sent under. */
object PaymentTable: UuidTable("payment") {
    val offeringId = reference("offering_id", OfferingTable).index()
    val sellerStarId = reference("seller_star_id", StarTable).index()
    val customerId = reference("customer_id", CustomerTable).nullable().index()
    val buyerStarId = reference("buyer_star_id", StarTable, ReferenceOption.SET_NULL).nullable().index()
    val buyerEmail = text("buyer_email")
    val provider = enumeration<PaymentProvider>("provider")
    val providerAccountId = text("provider_account_id")
    val amountCents = integer("amount_cents")
    val currency = text("currency")
    val platformFeeCents = integer("platform_fee_cents")
    val paymentStatus = enumeration<PaymentStatus>("payment_status")
    val fulfillmentStatus = enumeration<FulfillmentStatus>("fulfillment_status")
    val providerInvoiceId = text("provider_invoice_id").nullable().uniqueIndex()
    val providerPaymentId = text("provider_payment_id").nullable()
    val invoiceUrl = text("invoice_url").nullable()
    val dueAt = timestamp("due_at").nullable()
    val paidAt = timestamp("paid_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

fun ResultRow.toPayment() = Payment(
    paymentId = PaymentId(this[PaymentTable.id].value),
    offeringId = OfferingId(this[PaymentTable.offeringId].value),
    customerId = this[PaymentTable.customerId]?.let { CustomerId(it.value) },
    email = this[PaymentTable.buyerEmail].toEmailAddress(),
    amountCents = this[PaymentTable.amountCents],
    platformFeeCents = this[PaymentTable.platformFeeCents],
    paymentStatus = this[PaymentTable.paymentStatus],
    fulfillmentStatus = this[PaymentTable.fulfillmentStatus],
    dueAt = this[PaymentTable.dueAt],
    paidAt = this[PaymentTable.paidAt],
    createdAt = this[PaymentTable.createdAt],
    updatedAt = this[PaymentTable.updatedAt],
)

/**
 * Writes a payment [seller] requests for [offering] from [customer], at [priceCents] with Streetlight's [platformCut],
 * awaiting its invoice.
 */
fun UpdateBuilder<*>.createPayment(
    offering: Offering,
    seller: Seller,
    customer: ProviderCustomer,
    priceCents: Int,
    platformCut: Int,
    now: Instant,
) {
    this[PaymentTable.id] = Uuid.random()
    this[PaymentTable.offeringId] = offering.offeringId.value
    this[PaymentTable.sellerStarId] = seller.starId.value
    this[PaymentTable.customerId] = customer.customerId.value
    this[PaymentTable.buyerEmail] = customer.email.value
    this[PaymentTable.provider] = seller.provider
    this[PaymentTable.providerAccountId] = seller.providerAccountId
    this[PaymentTable.amountCents] = priceCents
    this[PaymentTable.currency] = offering.currency
    this[PaymentTable.platformFeeCents] = priceCents * platformCut / MAX_PLATFORM_CUT
    this[PaymentTable.paymentStatus] = PaymentStatus.Draft
    this[PaymentTable.fulfillmentStatus] = FulfillmentStatus.Open
    this[PaymentTable.createdAt] = now
    this[PaymentTable.updatedAt] = now
}

/** Writes the state of [invoice], the provider's invoice for the payment. */
fun UpdateBuilder<*>.updatePaymentInvoice(invoice: ProviderInvoice, now: Instant) {
    this[PaymentTable.providerInvoiceId] = invoice.invoiceId
    this[PaymentTable.paymentStatus] = invoice.status
    this[PaymentTable.invoiceUrl] = invoice.url?.value
    this[PaymentTable.dueAt] = invoice.dueAt
    this[PaymentTable.paidAt] = invoice.paidAt
    this[PaymentTable.updatedAt] = now
}
