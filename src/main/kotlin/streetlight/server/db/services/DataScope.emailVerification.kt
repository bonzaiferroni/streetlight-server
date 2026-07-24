package streetlight.server.db.services

import kampfire.api.Email
import kampfire.model.CallerId
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Token
import klutch.server.generateToken
import klutch.server.hashToken
import klutch.utils.eq
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.p
import kotlinx.html.stream.createHTML
import kotlinx.html.title
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.EmailStatus
import streetlight.model.data.toStarId
import streetlight.model.ui.VerifyEmailRoute
import streetlight.server.db.tables.AuthMeta
import streetlight.server.db.tables.AuthToken
import streetlight.server.db.tables.AuthTokenTable
import streetlight.server.db.tables.AuthType
import streetlight.server.db.tables.StarTable
import streetlight.server.model.DataScope
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

suspend fun DataScope.authEmailVerification(token: Token): Outcome<String> = tryOutcome {
    val now = Clock.System.now()
    val hashedToken = hashToken(token)
    val authToken = dao.authToken.readToken<AuthMeta.EmailVerification>(hashedToken, AuthType.EmailVerification)
        ?: return@tryOutcome Problem("Unable to find the request.")

    val account = dao.star.readAccount(authToken.starId) ?: error("account not found")
    if (account.email != authToken.meta.email) return@tryOutcome Problem("The email for this account has changed.")
    val expiredProblem = Problem("This request has expired, please try again.")
    if (authToken.consumedAt != null) {
        if (account.emailStatus == EmailStatus.Verified) return@tryOutcome Ok("Good news! This email has already been verified.")
        else return@tryOutcome expiredProblem
    }
    if (authToken.expiresAt < now) return@tryOutcome expiredProblem

    dao.authToken.verifyEmail(authToken)

    Ok("Success! This email has been verified.")
}

suspend fun DataScope.requestEmailVerification(callerId: CallerId): Outcome<Unit> {
    val account = transaction {
        dao.star.readAccount(callerId.toStarId())
    }
    if (account == null) return Problem("Account not found.")
    val email = account.email ?: return Problem("No user email.")
    if (account.emailStatus == EmailStatus.Verified) return Problem("This email is already verified.")
    val bouncedProblem = Problem("This address can't receive mail. Try a different one.")
    if (dao.authToken.readIsBounced(email)) return bouncedProblem
    val token = generateToken()
    val url = VerifyEmailRoute(token).toAbsolutePath()

    val response = client.postmark.sendEmail(
        to = email.value,
        subject = "Email verification",
        htmlBody = createHtmlBody(url),
        textBody = createTextBody(url)
    )

    if (response.errorCode == 406) {
        dao.authToken.createBouncedEmail(email, "postmark 406")
        dao.star.setEmailStatus(callerId, EmailStatus.Bounced)
        return bouncedProblem
    }
    if (response.errorCode != 0) return Problem("There was an internal error.")

    return when (createEmailVerificationToken(callerId, email, token)) {
        true -> Ok(Unit)
        else -> Problem("There was an internal error.")
    }
}

private suspend fun DataScope.createEmailVerificationToken(
    callerId: CallerId,
    email: Email,
    token: Token,
) = transaction {
    val now = Clock.System.now()
    dao.authToken.consumeAllTokensOfType(callerId, AuthType.EmailVerification)
    val hashedToken = hashToken(token)

    dao.authToken.createToken(
        AuthToken(
            tokenId = 0,
            starId = callerId.toStarId(),
            hashedToken = hashedToken,
            authType = AuthType.EmailVerification,
            meta = AuthMeta.EmailVerification(email),
            consumedAt = null,
            expiresAt = now + EmailVerificationInterval,
            createdAt = now
        )
    ).insertedCount == 1
}

val EmailVerificationInterval = 1.days

private fun createHtmlBody(url: String) = createHTML().html {
    head {
        title("Verify your email")
    }
    body {
        h1 { +"Almost there" }
        p {
            +"Confirm your address by clicking the link below."
        }
        a(href = url) {
            +"Verify email"
        }
        p {
            +"This link expires in 24 hours. If you didn't request this, you can ignore this message."
        }
    }
}

private fun createTextBody(url: String) = """
Confirm your email address by visiting the link below:

$url

This link expires in 24 hours. If you didn't request this, you can ignore this message.
""".trimIndent()