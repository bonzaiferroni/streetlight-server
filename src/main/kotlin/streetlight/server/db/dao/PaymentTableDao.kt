package streetlight.server.db.dao

import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.Offering
import streetlight.model.data.PaymentId
import streetlight.model.data.PaymentProvider
import streetlight.server.db.tables.PaymentTable
import streetlight.server.db.tables.createPayment
import streetlight.server.db.tables.toPayment
import streetlight.server.db.tables.updatePaymentInvoice
import streetlight.server.model.Customer
import streetlight.server.model.ProviderInvoice
import streetlight.server.model.Seller
import kotlin.time.Clock

class PaymentTableDao: DbService() {
    suspend fun create(offering: Offering, seller: Seller, customer: Customer) = dbQuery {
        PaymentTable.insertReturning { it.createPayment(offering, seller, customer, Clock.System.now()) }
            .single().toPayment()
    }

    /** Writes [invoice] to the payment [paymentId]. */
    suspend fun updateInvoice(paymentId: PaymentId, invoice: ProviderInvoice) = dbQuery {
        PaymentTable.updateReturning(where = { PaymentTable.id.eq(paymentId) }) {
            it.updatePaymentInvoice(invoice, Clock.System.now())
        }.single().toPayment()
    }

    /** Writes [invoice] to the payment it was sent for, or returns `null` when no payment holds it. */
    suspend fun updateInvoice(provider: PaymentProvider, invoice: ProviderInvoice) = dbQuery {
        PaymentTable.updateReturning(where = {
            PaymentTable.provider.eq(provider) and PaymentTable.providerInvoiceId.eq(invoice.invoiceId)
        }) { it.updatePaymentInvoice(invoice, Clock.System.now()) }
            .singleOrNull()?.toPayment()
    }
}
