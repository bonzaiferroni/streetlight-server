package streetlight.server.service

import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Url
import streetlight.model.Api
import streetlight.model.data.CapabilityStatus
import streetlight.model.data.SellerStatus
import streetlight.model.data.SellerConfiguration
import streetlight.model.data.SellerOnboarding
import streetlight.model.data.StarId
import streetlight.model.data.viableEmail
import streetlight.model.ui.StarConfigRoute
import streetlight.server.db.dao.tryOutcome
import streetlight.server.model.DataScope
import streetlight.server.model.ProviderEvent
import streetlight.server.model.toSellerStatus

/** A link to the provider's onboarding for [starId], creating their seller account from [onboarding] when they have none. */
suspend fun DataScope.onboardSeller(starId: StarId, onboarding: SellerOnboarding): Outcome<Url> = tryOutcome {
    val provider = client.payment.provider
    if (dao.seller.readSeller(starId, provider) == null) {
        val email = dao.star.readAccount(starId)?.viableEmail
        val configuration = SellerConfiguration.Standard
        val account = client.payment.createAccount(starId, configuration, onboarding.country, email)
        dao.seller.create(starId, provider, configuration, account)
    }
    refreshSellerLink(starId)
}

/** A fresh link to the provider's onboarding for the seller account [starId] already has. */
suspend fun DataScope.refreshSellerLink(starId: StarId): Outcome<Url> = tryOutcome {
    val seller = dao.seller.readSeller(starId, client.payment.provider)
        ?: return@tryOutcome Problem("No seller account found.")
    val returnUrl = "$hostAddress${StarConfigRoute.toRelativePath()}"
    val refreshUrl = "$hostAddress${Api.Sellers.Onboard.Refresh.path}"
    Ok(client.payment.createOnboardingLink(seller.providerAccountId, returnUrl, refreshUrl))
}

/**
 * The status of the seller account of [starId], or `null` when they have none.
 *
 * An account not yet active for charges and payouts is read again from the provider first.
 */
suspend fun DataScope.readSellerStatus(starId: StarId): Outcome<SellerStatus?> = tryOutcome {
    val provider = client.payment.provider
    val seller = dao.seller.readSeller(starId, provider) ?: return@tryOutcome Ok(null)
    if (seller.chargeStatus == CapabilityStatus.Active && seller.payoutStatus == CapabilityStatus.Active)
        return@tryOutcome Ok(seller.toSellerStatus())
    dao.seller.update(provider, client.payment.readAccount(seller.providerAccountId))
    Ok(dao.seller.readSeller(starId, provider)?.toSellerStatus())
}

/** Applies [event] from the provider to the seller it concerns. */
suspend fun DataScope.applyProviderEvent(event: ProviderEvent) {
    val provider = client.payment.provider
    when (event) {
        is ProviderEvent.AccountUpdated -> {
            if (!dao.seller.isSeller(provider, event.accountId)) return
            dao.seller.update(provider, client.payment.readAccount(event.accountId))
        }
        is ProviderEvent.AccountRemoved -> dao.seller.delete(provider, event.accountId)
        is ProviderEvent.Unhandled -> log("unhandled ${provider.name} event: ${event.type}")
    }
}
