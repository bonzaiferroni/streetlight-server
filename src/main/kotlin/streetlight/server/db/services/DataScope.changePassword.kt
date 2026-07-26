package streetlight.server.db.services

import kampfire.api.Email
import kampfire.api.Password
import kampfire.api.toValidOutcome
import kampfire.model.CallerId
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.PasswordChange
import kampfire.model.PasswordResetRedemption
import kampfire.model.Problem
import klutch.db.model.SessionId
import klutch.db.services.SessionService
import klutch.server.generateToken
import klutch.server.hashPassword
import klutch.server.hashToken
import klutch.server.verifyPassword
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.p
import kotlinx.html.stream.createHTML
import kotlinx.html.title
import streetlight.model.data.EmailStatus
import streetlight.model.data.StarId
import streetlight.model.data.toStarId
import streetlight.model.ui.AccountLockdownRoute
import streetlight.server.db.tables.AuthTokenType
import streetlight.server.external.PostmarkResponse
import streetlight.server.model.DataScope
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

suspend fun DataScope.changePassword(
    callerId: CallerId,
    request: PasswordChange,
    sessionId: SessionId,
    sessionService: SessionService,
): Outcome<Unit> = tryOutcome {
    val password = when (val outcome = request.newPassword.toValidOutcome()) {
        is Ok -> outcome.data
        is Problem -> return@tryOutcome outcome
    }

    val account = dao.star.readAccount(callerId.toStarId()) ?: error("account not found")
    val passwordHash = dao.star.readPasswordHash(callerId) ?: error("password not found")

    if (!verifyPassword(request.currentPassword, passwordHash))
        return@tryOutcome Problem("Your current password is not correct.")

    applyPasswordChange(
        starId = account.starId,
        password = password,
        email = account.email.takeIf { account.emailStatus == EmailStatus.Verified },
        sessionService = sessionService,
        sessionIdToSpare = sessionId,
    )

    Ok(Unit)
}

internal suspend fun DataScope.applyPasswordChange(
    starId: StarId,
    password: Password,
    email: Email?,
    sessionService: SessionService,
    sessionIdToSpare: SessionId? = null,
    tokenIdToConsume: Long? = null,
    now: Instant = Clock.System.now(),
): Boolean {
    val passwordHash = hashPassword(password)

    val applied = transaction {
        if (tokenIdToConsume != null) {
            val updated = dao.authToken.consumeToken(tokenIdToConsume, now)
            if (updated != 1) return@transaction false
        }
        dao.star.setPassword(starId, passwordHash)
        sessionService.deleteSessions(starId, sessionIdToSpare)
        true
    }

    if (!applied) return false
    if (email == null) return true

    val token = generateToken()
    val url = AccountLockdownRoute(token).toAbsolutePath()

    val response = client.postmark.sendEmail(
        to = email.value,
        subject = "Password changed",
        htmlBody = createPasswordChangedHtmlBody(url),
        textBody = createPasswordChangedTextBody(url),
    )

    if (recordIfPostmarkError(response, email)) return true

    createTokenOrThrow(starId, token, email, AuthTokenType.AccountLockdown, AccountLockdownInterval)

    return true
}

internal suspend fun DataScope.recordIfPostmarkError(
    response: PostmarkResponse,
    email: Email,
): Boolean {
    if (response.errorCode == 0) return false
    if (response.errorCode == 406) {
        dao.bouncedEmail.createBouncedEmail(email, "postmark 406")
        dao.star.setEmailStatus(email, EmailStatus.Bounced)
    }
    log.error { "Postmark error: ${response.errorCode}" }
    return true
}

private val AccountLockdownInterval = 2.days

private fun createPasswordChangedHtmlBody(url: String) = createHTML().html {
    head {
        title("Streetlight Password Changed")
    }
    body {
        p {
            +"The password for your Streetlight account has changed."
        }
        p {
            +"If this was you, no action is needed."
        }
        p {
            +"If it wasn't, secure your account now — this will sign out every device and let you set a new password."
        }
        a(href = url) {
            +"This wasn't me"
        }
    }
}

private fun createPasswordChangedTextBody(url: String) = """
The password for your Streetlight account has changed.

If this was you, no action is needed.

If it wasn't, secure your account now. This will sign out every device and let you set a new password:

$url
""".trimIndent()