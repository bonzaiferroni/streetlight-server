package streetlight.server.db.services

import kampfire.api.Email
import kampfire.api.toValidOutcome
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.PasswordResetRedemption
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
import streetlight.model.data.EmailStatus
import streetlight.model.data.StarId
import streetlight.model.ui.PasswordResetRoute
import streetlight.server.db.tables.AuthTokenType
import streetlight.server.model.DataScope
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

suspend fun DataScope.redeemPasswordReset(
    request: PasswordResetRedemption,
    sessionService: SessionService,
): Outcome<Unit> = tryOutcome {
    val now = Clock.System.now()

    val password = when (val outcome = request.password.toValidOutcome()) {
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
        password = password,
        email = authToken.email,
        sessionService = sessionService,
        tokenIdToConsume = authToken.tokenId,
        now = now,
    )

    if (!applied) return@tryOutcome expiredProblem

    Ok(Unit)
}

suspend fun DataScope.requestPasswordReset(email: Email): Outcome<Unit> = tryOutcome(Ok(Unit)) {
    // Ok(Unit) is returned in all branches for account security reasons
    if (dao.bouncedEmail.readIsBounced(email)) return@tryOutcome Ok(Unit)
    val account = dao.star.readAccount(email) ?: return@tryOutcome Ok(Unit)

    sendPasswordReset(account.starId, email)

    Ok(Unit)
}

internal suspend fun DataScope.sendPasswordReset(
    starId: StarId,
    email: Email,
    interval: Duration = PasswordResetInterval,
) {
    val token = generateToken()
    val url = PasswordResetRoute(token).toAbsolutePath()

    val response = client.postmark.sendEmail(
        to = email.value,
        subject = "Password reset",
        htmlBody = createPasswordResetHtmlBody(url),
        textBody = createPasswordResetTextBody(url),
    )

    if (recordIfPostmarkError(response, email)) error("Postmark error: ${response.errorCode}")

    createTokenOrThrow(starId, token, email, AuthTokenType.PasswordReset, interval)
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