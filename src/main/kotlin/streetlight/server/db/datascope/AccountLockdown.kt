package streetlight.server.db.datascope

import kampfire.api.EmailAddress
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Token
import klutch.db.services.SessionService
import klutch.server.hashToken
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.p
import kotlinx.html.stream.createHTML
import kotlinx.html.title
import streetlight.model.data.AuthTokenType
import streetlight.model.data.EmailStatus
import streetlight.server.db.services.tryOutcome
import streetlight.server.model.DataScope
import streetlight.server.model.Email
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Locks the account of a lockdown [token], sent when its password or email changed.
 *
 * The account is signed out everywhere and its password disabled, and the address the token was sent to is
 * restored as its verified email, then sent a reset link. When another account has since verified that address,
 * it cannot be restored, and the address is told to contact support.
 */
suspend fun DataScope.redeemAccountLockdown(token: Token, sessionService: SessionService): Outcome<Unit> = tryOutcome {
    val now = Clock.System.now()
    val hashedToken = hashToken(token)
    val authToken = dao.authToken.readToken(hashedToken, AuthTokenType.AccountLockdown)
        ?: return@tryOutcome Problem("This link is not valid.")

    // No same-address guard here. A lockdown token is issued when a user email is changed.

    if (authToken.consumedAt != null)
        return@tryOutcome Ok(Unit)
    if (authToken.expiresAt < now)
        return@tryOutcome Problem("This link has expired. Contact us at ${appEmail.support} for help securing your account.")

    val canRevert = transaction {
        val holder = dao.star.readAccount(authToken.email)?.takeIf { it.starId != authToken.starId }
        val canRevert = holder == null || holder.emailStatus != EmailStatus.Verified

        dao.authToken.consumeToken(authToken.tokenId, now)
        dao.authToken.consumeAllUserTokens(
            starId = authToken.starId,
            exceptLockdownBefore = authToken.tokenId,
        )

        if (canRevert) {
            if (holder != null) dao.star.setEmailNotOwned(holder.starId)
            dao.star.setEmail(authToken.starId, authToken.email, EmailStatus.Verified)
        } else {
            log.error {
                "AccountLockdown could not revert email: lockedStarId=${authToken.starId}, " +
                        "holderStarId=${holder.starId}, tokenId=${authToken.tokenId}"
            }
        }

        dao.star.disablePassword(authToken.starId)
        sessionService.deleteSessions(authToken.starId, null)
        canRevert
    }

    if (!canRevert) {
        // td: send an urgent message to support other than the log
        sendLockdownSupportNotice(authToken.email)
        return@tryOutcome Ok(Unit)
    }

    sendPasswordReset(authToken.starId, authToken.email, AccountLockdownResetInterval)

    Ok(Unit)
}

private val AccountLockdownResetInterval = 1.days

internal suspend fun DataScope.sendLockdownSupportNotice(email: EmailAddress) {
    val response = client.postmark.sendEmail(
        Email(
            from = appEmail.support,
            to = email,
            subject = "Your Streetlight account is locked",
            htmlBody = createLockdownSupportHtmlBody(appEmail.support),
            textBody = createLockdownSupportTextBody(appEmail.support),
        )
    )

    recordEmailBounced(response, email)
    if (response.errorCode != 0) {
        log.error { "Failed to send lockdown support notice" }
    }
}

private fun createLockdownSupportHtmlBody(supportAddress: EmailAddress) = createHTML().html {
    head {
        title("Your Streetlight account is locked")
    }
    body {
        h1 { +"Account locked" }
        p {
            +"The account has been signed out on every device and password sign-in has been disabled."
        }
        p {
            +"We weren't able to restore this address to the account, so we can't send you a link to set a new password. Please contact us and we'll help you recover it."
        }
        a(href = "mailto:$supportAddress") {
            +supportAddress.value
        }
    }
}

private fun createLockdownSupportTextBody(supportAddress: EmailAddress) = """
The account has been signed out on every device and password sign-in has been disabled.

We weren't able to restore this email address to the account, so we can't send you a link to set a new password. Please contact us at the address below and we'll help you recover it.

$supportAddress
""".trimIndent()