package streetlight.server.db.datascope

import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Token
import klutch.server.hashToken
import streetlight.model.data.AuthTokenType
import streetlight.server.db.services.tryOutcome
import streetlight.server.model.DataScope
import kotlin.time.Clock

/** Marks the email of an account as not owned, for a [token] sent to that address by someone who did not sign up. */
suspend fun DataScope.redeemAccountNotOwned(token: Token): Outcome<Unit> = tryOutcome {
    val now = Clock.System.now()
    val hashedToken = hashToken(token)
    val authToken = dao.authToken.readToken(hashedToken, AuthTokenType.AccountNotOwned)
        ?: return@tryOutcome Problem("This link is not valid.")

    val account = dao.star.readAccount(authToken.starId)
        ?: return@tryOutcome Ok(Unit)

    if (account.email != authToken.email) return@tryOutcome Ok(Unit)

    if (authToken.consumedAt != null) return@tryOutcome Ok(Unit)
    if (authToken.expiresAt < now)
        return@tryOutcome Problem("This link has expired. Contact us at ${appEmail.support} to remove your address.")

    transaction {
        dao.authToken.consumeToken(authToken.tokenId, now)
        dao.authToken.consumeAllTokensForEmail(authToken.starId, authToken.email)
        dao.star.setEmailNotOwned(authToken.starId)
        // td: send a UI-delivered message to the user
    }

    Ok(Unit)
}