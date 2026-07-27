package streetlight.server.db.services

import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Token
import klutch.server.hashToken
import streetlight.model.data.AuthTokenType
import streetlight.server.model.DataScope
import kotlin.time.Clock

suspend fun DataScope.redeemAccountNotOwned(token: Token, supportAddress: String): Outcome<Unit> = tryOutcome {
    val now = Clock.System.now()
    val hashedToken = hashToken(token)
    val authToken = dao.authToken.readToken(hashedToken, AuthTokenType.AccountNotOwned)
        ?: return@tryOutcome Problem("This link is not valid.")

    val account = dao.star.readAccount(authToken.starId)
        ?: return@tryOutcome Ok(Unit)

    if (account.email != authToken.email) return@tryOutcome Ok(Unit)

    if (authToken.consumedAt != null) return@tryOutcome Ok(Unit)
    if (authToken.expiresAt < now)
        return@tryOutcome Problem("This link has expired. Contact us at $supportAddress to remove your address.")

    transaction {
        dao.authToken.consumeToken(authToken.tokenId, now)
        dao.authToken.consumeAllUserTokens(authToken.starId)
        dao.star.setEmailNotOwned(authToken.starId)
        // td: send a UI-delivered message to the user
    }

    Ok(Unit)
}