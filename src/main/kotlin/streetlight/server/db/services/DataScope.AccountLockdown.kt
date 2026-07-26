package streetlight.server.db.services

import kampfire.api.Email
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
import streetlight.model.data.EmailStatus
import streetlight.server.db.tables.AuthTokenType
import streetlight.server.model.DataScope
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

suspend fun DataScope.redeemAccountLockdown(
    token: Token,
    sessionService: SessionService,
    supportAddress: String,
): Outcome<Unit> = tryOutcome {
    val now = Clock.System.now()
    val hashedToken = hashToken(token)
    val authToken = dao.authToken.readToken(hashedToken, AuthTokenType.AccountLockdown)
        ?: return@tryOutcome Problem("This link is not valid.")

    // No same-address guard here. A lockdown token is issued when a user email is changed.

    if (authToken.consumedAt != null)
        return@tryOutcome Ok(Unit)
    if (authToken.expiresAt < now)
        return@tryOutcome Problem("This link has expired. Contact us at $supportAddress for help securing your account.")

    val canRevert = transaction {
        val holder = dao.star.readAccount(authToken.email)
            ?.takeIf { it.starId != authToken.starId }
        val canRevert = holder == null || holder.emailStatus != EmailStatus.Verified

        dao.authToken.consumeAllUserTokens(authToken.starId)

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
        sendLockdownSupportNotice(authToken.email, supportAddress)
        return@tryOutcome Ok(Unit)
    }

    sendPasswordReset(authToken.starId, authToken.email, AccountLockdownResetInterval)

    Ok(Unit)
}

private val AccountLockdownResetInterval = 1.days

internal suspend fun DataScope.sendLockdownSupportNotice(
    email: Email,
    supportAddress: String,
) {
    val response = client.postmark.sendEmail(
        to = email.value,
        subject = "Your Streetlight account is locked",
        htmlBody = createLockdownSupportHtmlBody(supportAddress),
        textBody = createLockdownSupportTextBody(supportAddress),
    )

    if (recordIfPostmarkError(response, email)) {
        log.error { "Failed to send lockdown support notice" }
    }
}

private fun createLockdownSupportHtmlBody(supportAddress: String) = createHTML().html {
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
            +supportAddress
        }
    }
}

private fun createLockdownSupportTextBody(supportAddress: String) = """
The account has been signed out on every device and password sign-in has been disabled.

We weren't able to restore this email address to the account, so we can't send you a link to set a new password. Please contact us at the address below and we'll help you recover it.

$supportAddress
""".trimIndent()