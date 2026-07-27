package streetlight.server.db.services

import kampfire.api.Email
import kampfire.api.toEmail
import kampfire.model.Token
import klutch.server.hashToken
import streetlight.model.data.AuthTokenType
import streetlight.model.data.EmailStatus
import streetlight.model.data.StarId
import streetlight.server.db.tables.AuthToken
import streetlight.server.external.PostmarkBounce
import streetlight.server.model.DataScope
import kotlin.time.Clock
import kotlin.time.Duration

internal suspend fun DataScope.createTokenOrThrow(
    starId: StarId,
    token: Token,
    email: Email,
    tokenType: AuthTokenType,
    interval: Duration,
    consumePrior: Boolean = true,
) {
    val now = Clock.System.now()
    val hashedToken = hashToken(token)

    transaction {
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
        if (!isSuccess) error("unable to create token: $tokenType")
    }
}

suspend fun DataScope.recordBounce(bounce: PostmarkBounce) {
    val email = bounce.email.toEmail()

    dao.bouncedEmail.createBouncedEmail(
        email = email,
        reason = "${bounce.type}: ${bounce.description}",
    )
    dao.star.setEmailStatus(email, EmailStatus.Bounced)
}