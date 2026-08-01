package streetlight.server.db.services.datascope

import kampfire.api.EmailAddress
import kampfire.api.deobfuscatePassword
import kampfire.api.toValidOutcome
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.PasswordResetRequest
import kampfire.model.Problem
import klutch.db.services.SessionService
import klutch.server.generateToken
import klutch.server.hashToken
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.p
import kotlinx.html.stream.createHTML
import kotlinx.html.title
import streetlight.model.data.AuthTokenType
import streetlight.model.data.StarId
import streetlight.model.ui.PasswordResetRoute
import streetlight.server.db.services.tryOutcome
import streetlight.server.model.DataScope
import streetlight.server.model.Email
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

suspend fun DataScope.redeemPasswordReset(
    request: PasswordResetRequest,
    sessionService: SessionService,
): Outcome<Unit> = tryOutcome {
    val now = Clock.System.now()

    val newPassword = when (val outcome = request.password.deobfuscatePassword().toValidOutcome()) {
        is Ok -> outcome.data
        is Problem -> return@tryOutcome outcome
    }

    val hashedToken = hashToken(request.token)
    val authToken = dao.authToken.readToken(hashedToken, AuthTokenType.PasswordReset)
        ?: return@tryOutcome Problem("This reset link is not valid. Please request a new one.")

    val expiredProblem = Problem("This reset link has expired. Please request a new one.")
    if (authToken.consumedAt != null) return@tryOutcome expiredProblem
    if (authToken.expiresAt < now) return@tryOutcome expiredProblem

    val account = dao.star.readAccount(authToken.starId) ?: return@tryOutcome expiredProblem
    if (account.email != authToken.email)
        return@tryOutcome Problem("The email for this account has changed. Please request a new reset link.")

    val applied = applyPasswordChange(
        starId = authToken.starId,
        newPassword = newPassword,
        email = authToken.email,
        sessionService = sessionService,
        tokenIdToConsume = authToken.tokenId,
        now = now,
    )

    if (!applied) return@tryOutcome expiredProblem

    Ok(Unit)
}

suspend fun DataScope.requestPasswordReset(email: EmailAddress): Outcome<Unit> = tryOutcome(Ok(Unit)) {
    // Ok(Unit) is returned in all branches for account security reasons
    if (dao.bouncedEmail.readIsBounced(email)) return@tryOutcome Ok(Unit)
    val account = dao.star.readAccount(email) ?: return@tryOutcome Ok(Unit)

    sendPasswordReset(account.starId, email)

    Ok(Unit)
}

internal suspend fun DataScope.sendPasswordReset(
    starId: StarId,
    email: EmailAddress,
    interval: Duration = PasswordResetInterval,
): Boolean {
    val token = generateToken()
    val url = PasswordResetRoute(token).toAbsolutePath()

    val response = client.postmark.sendEmail(
        Email(
            from = appEmail.support,
            to = email,
            subject = "Password reset",
            htmlBody = createPasswordResetHtmlBody(url),
            textBody = createPasswordResetTextBody(url),
        )
    )

    recordEmailBounced(response, email)
    if (response.errorCode != 0) return false

    createToken(starId, token, email, AuthTokenType.PasswordReset, interval)
    return true
}

private val PasswordResetInterval = 10.minutes

private fun createPasswordResetHtmlBody(url: String) = createHTML().html {
    head {
        title("Reset Your Password")
    }
    body {
        p {
            +"Click the link below to reset your password."
        }
        a(href = url) {
            +"Reset my Streetlight password"
        }
        p {
            +expiryMessage
        }
    }
}

private fun createPasswordResetTextBody(url: String) = """
Reset your password by visiting the link below:

$url

$expiryMessage
""".trimIndent()

private val expiryMessage = "If you didn't request this, no action is needed. Your password hasn't been changed and this link will expire shortly."