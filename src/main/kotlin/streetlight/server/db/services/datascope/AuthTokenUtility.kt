package streetlight.server.db.services.datascope

import kampfire.api.EmailAddress
import kampfire.api.toEmailAddress
import kampfire.model.Token
import klutch.server.hashToken
import streetlight.model.data.AuthTokenType
import streetlight.model.data.EmailStatus
import streetlight.model.data.StarId
import streetlight.server.db.tables.AuthToken
import streetlight.server.external.PostmarkBounce
import streetlight.server.model.DataScope
import streetlight.server.model.SendEmailResult
import kotlin.time.Clock
import kotlin.time.Duration

internal suspend fun DataScope.createToken(
    starId: StarId,
    token: Token,
    email: EmailAddress,
    tokenType: AuthTokenType,
    interval: Duration,
    consumePrior: Boolean = true,
): Boolean {
    val now = Clock.System.now()
    val hashedToken = hashToken(token)

    if (tokenType == AuthTokenType.AccountLockdown && consumePrior)
        error("Lockdown tokens should never consume prior tokens.")

    return transaction {
        if (consumePrior) {
            dao.authToken.consumeAllTokensOfType(starId, tokenType)
        }
        val isSuccess = dao.authToken.createToken(
            AuthToken(
                tokenId = 0,
                starId = starId,
                hashedToken = hashedToken,
                tokenType = tokenType,
                email = email,
                consumedAt = null,
                expiresAt = now + interval,
                createdAt = now
            )
        ).insertedCount == 1
        isSuccess
    }
}

suspend fun DataScope.recordBounce(bounce: PostmarkBounce) {
    val email = bounce.email.toEmailAddress()

    dao.bouncedEmail.createBouncedEmail(
        email = email,
        reason = "${bounce.type}: ${bounce.description}",
    )
    dao.star.setEmailStatus(email, EmailStatus.Bounced)
}

internal suspend fun DataScope.recordEmailBounced(
    response: SendEmailResult,
    email: EmailAddress,
) {
    if (response.errorCode != 0) {
        log.error { "Postmark error: ${response.errorCode}" }
    }
    if (response.errorCode == 406) {
        dao.bouncedEmail.createBouncedEmail(email, "postmark 406")
        dao.star.setEmailStatus(email, EmailStatus.Bounced)
    }
}