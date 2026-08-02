package streetlight.server.db.datascope

import kampfire.api.EmailAddress
import kampfire.api.Password
import kampfire.api.deobfuscatePassword
import kampfire.api.toValidOutcome
import kampfire.model.AuthProblem
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.PasswordChange
import kampfire.model.Problem
import klutch.db.model.CallerId
import klutch.db.model.SessionId
import klutch.db.services.SessionService
import klutch.server.generateToken
import klutch.server.hashPassword
import klutch.server.verifyPassword
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.p
import kotlinx.html.stream.createHTML
import kotlinx.html.title
import streetlight.model.data.AuthTokenType
import streetlight.model.data.EmailStatus
import streetlight.model.data.StarId
import streetlight.model.ui.AccountLockdownRoute
import streetlight.server.db.services.tryOutcome
import streetlight.server.model.DataScope
import streetlight.server.model.Email
import streetlight.server.utils.toStarId
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

suspend fun DataScope.changePasswordFromSession(
    callerId: CallerId,
    request: PasswordChange,
    sessionId: SessionId,
    sessionService: SessionService,
): Outcome<Unit> = tryOutcome {
    val newPassword = when (val outcome = request.newPassword.deobfuscatePassword().toValidOutcome()) {
        is Ok -> outcome.data
        is Problem -> return@tryOutcome outcome
    }

    val account = dao.star.readAccount(callerId.toStarId()) ?: error("account not found")
    val email = account.email

    // current password check only applies to accounts with an email
    if (request.passwordNow == null && email != null) return@tryOutcome AuthProblem.CurrentPasswordInvalid

    // there should always be a current password here
    // guest accounts shouldn't reach this function, and recovery from a disabled password calls a different function
    val currentPasswordHash = dao.star.readPasswordHash(account.starId) ?: error("password not found")

    request.passwordNow?.let {
        val currentPassword = it.deobfuscatePassword()
        if (!verifyPassword(currentPassword, currentPasswordHash))
            return@tryOutcome AuthProblem.CurrentPasswordInvalid
    }

    if (verifyPassword(newPassword, currentPasswordHash)) {
        return@tryOutcome AuthProblem.NewPasswordRequired
    }

    applyPasswordChange(
        starId = account.starId,
        newPassword = newPassword,
        email = account.email.takeIf { account.emailStatus == EmailStatus.Verified },
        sessionService = sessionService,
        sessionIdToSpare = sessionId,
    )

    Ok(Unit)
}

// this function services changePasswordFromSession and redeemPasswordReset

internal suspend fun DataScope.applyPasswordChange(
    starId: StarId,
    newPassword: Password,
    email: EmailAddress?,
    sessionService: SessionService,
    sessionIdToSpare: SessionId? = null,
    tokenIdToConsume: Long? = null,
    now: Instant = Clock.System.now(),
): Boolean {
    val passwordHash = hashPassword(newPassword)

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
        Email(
            from = appEmail.support,
            to = email,
            subject = "Password changed",
            htmlBody = createPasswordChangedHtmlBody(url),
            textBody = createPasswordChangedTextBody(url),
        )
    )

    recordEmailBounced(response, email)
    if (response.errorCode != 0) return true

    createToken(starId, token, email, AuthTokenType.AccountLockdown, AccountLockdownInterval, consumePrior = false)

    return true
}

internal val AccountLockdownInterval = 2.days

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