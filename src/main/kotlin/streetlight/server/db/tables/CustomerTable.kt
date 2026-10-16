package streetlight.server.db.tables

import kampfire.api.EmailAddress
import kampfire.api.toEmailAddress
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.CustomerId
import streetlight.model.data.PaymentProvider
import streetlight.model.data.StarId
import streetlight.server.model.Customer
import streetlight.server.model.Seller
import kotlin.time.Instant

/** The buyers of each seller, one per address on the seller's provider account. */
object CustomerTable: UuidTable("customer") {
    val starId = reference("star_id", StarTable).index()
    val provider = enumeration<PaymentProvider>("provider")
    val providerAccountId = text("provider_account_id")
    val providerCustomerId = text("provider_customer_id")
    val email = text("email")
    val name = text("name").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        uniqueIndex(provider, providerAccountId, email)
    }
}

fun ResultRow.toCustomer() = Customer(
    customerId = CustomerId(this[CustomerTable.id].value),
    starId = StarId(this[CustomerTable.starId].value),
    provider = this[CustomerTable.provider],
    providerAccountId = this[CustomerTable.providerAccountId],
    providerCustomerId = this[CustomerTable.providerCustomerId],
    email = this[CustomerTable.email].toEmailAddress(),
    name = this[CustomerTable.name],
)

/** Writes the customer [customerId] of [seller] at [email], known to the provider as [providerCustomerId]. */
fun UpdateBuilder<*>.createCustomer(
    customerId: CustomerId,
    seller: Seller,
    providerCustomerId: String,
    email: EmailAddress,
    now: Instant,
) {
    this[CustomerTable.id] = customerId.value
    this[CustomerTable.starId] = seller.starId.value
    this[CustomerTable.provider] = seller.provider
    this[CustomerTable.providerAccountId] = seller.providerAccountId
    this[CustomerTable.providerCustomerId] = providerCustomerId
    this[CustomerTable.email] = email.value
    this[CustomerTable.createdAt] = now
    this[CustomerTable.updatedAt] = now
}
