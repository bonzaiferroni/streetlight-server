package streetlight.server.service

import kampfire.api.toEmailAddress
import kampfire.model.HttpProblem
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import klutch.db.model.CallerId
import streetlight.model.data.CapabilityStatus
import streetlight.model.data.CustomerId
import streetlight.model.data.Payment
import streetlight.model.data.PaymentEdit
import streetlight.server.db.dao.tryOutcome
import streetlight.server.model.ProviderCustomer
import streetlight.server.model.DataScope
import streetlight.server.model.InvoiceRequest
import streetlight.server.model.Seller
import streetlight.server.utils.toStarId

/**
 * Creates a payment of [callerId] for the offering [edit] names, at its price and cut, and sends its invoice. The
 * customer is [edit]'s, or the seller's customer at its email, created when there is none, and takes [edit]'s name
 * when it has none.
 */
suspend fun DataScope.createPayment(callerId: CallerId, edit: PaymentEdit): Outcome<Payment> = tryOutcome {
    if (!edit.validity.isValid) return@tryOutcome HttpProblem.BadRequest
    val starId = callerId.toStarId()
    val offering = dao.offering.readStarOffering(starId, edit.offeringId)
        ?: return@tryOutcome HttpProblem.NotFound
    val seller = dao.seller.readSeller(starId, client.payment.provider)
        ?: return@tryOutcome PaymentProblem.NoSeller
    if (seller.chargeStatus != CapabilityStatus.Active) return@tryOutcome PaymentProblem.ChargesInactive

    // read or create customer
    val priceCents = edit.priceCents ?: return@tryOutcome HttpProblem.BadRequest
    val name = edit.name?.trim()?.takeIf { it.isNotEmpty() }
    val customer = when (val customerId = edit.customerId) {
        null -> readOrCreateCustomer(seller, edit.email ?: return@tryOutcome HttpProblem.BadRequest, name)
        else -> dao.customer.readCustomer(customerId, starId) ?: return@tryOutcome HttpProblem.NotFound
    }.let { customer ->
        if (customer.name != null || name == null) return@let customer
        client.payment.updateCustomerName(seller.providerAccountId, customer.providerCustomerId, name)
        dao.customer.updateName(customer.customerId, name)
    }

    // record and send invoice
    log("creating payment: ${offering.title}")
    val payment = dao.payment.create(offering, seller, customer, priceCents, edit.platformCut)
    val invoice = client.payment.sendInvoice(seller.providerAccountId, InvoiceRequest(
        paymentId = payment.paymentId,
        providerCustomerId = customer.providerCustomerId,
        description = offering.title,
        amountCents = payment.amountCents,
        currency = offering.currency,
        platformFeeCents = payment.platformFeeCents,
        daysUntilDue = INVOICE_DAYS_UNTIL_DUE,
    ))
    Ok(dao.payment.updateInvoice(payment.paymentId, invoice))
}

/** The customer at [email] on [seller]'s account, created with the provider as [name] when there is none. */
private suspend fun DataScope.readOrCreateCustomer(seller: Seller, email: String, name: String?): ProviderCustomer {
    val emailAddress = email.toEmailAddress()
    dao.customer.readCustomer(seller, emailAddress)?.let { return it }
    val customerId = CustomerId.random()
    val providerCustomerId = client.payment.createCustomer(seller.providerAccountId, customerId, emailAddress, name)
    return dao.customer.create(customerId, seller, providerCustomerId, emailAddress, name)
}

/** The days a customer has to pay an invoice once it is sent. */
private const val INVOICE_DAYS_UNTIL_DUE = 30

object PaymentProblem {
    val NoSeller = Problem("Connect a seller account before sending an invoice.")
    val ChargesInactive = Problem("Your seller account can't take payments yet.")
}
