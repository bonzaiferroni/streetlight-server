package streetlight.server.db.services

import kampfire.model.CallerId
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Token
import klutch.server.generateToken
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
import streetlight.model.data.toStarId
import streetlight.model.ui.NotOwnedEmailRoute
import streetlight.model.ui.VerifyEmailRoute
import streetlight.server.db.tables.AuthTokenType
import streetlight.server.model.DataScope
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

suspend fun DataScope.requestEmailVerification(callerId: CallerId): Outcome<Unit> = tryOutcome {
    val starId = callerId.toStarId()
    val account = dao.star.readAccount(starId) ?: return@tryOutcome Problem("Account not found.")
    val email = account.email ?: return@tryOutcome Problem("No user email.")
    if (account.emailStatus == EmailStatus.Verified) return@tryOutcome Problem("This email is already verified.")
    val bouncedProblem = Problem("This address can't receive mail. Try a different one.")
    if (dao.bouncedEmail.readIsBounced(email)) return@tryOutcome bouncedProblem
    val verifyToken = generateToken()
    val disavowToken = generateToken()
    val verifyUrl = VerifyEmailRoute(verifyToken).toAbsolutePath()
    val notOwnedUrl = NotOwnedEmailRoute(disavowToken).toAbsolutePath()

    val response = client.postmark.sendEmail(
        to = email.value,
        subject = "Email verification",
        htmlBody = createEmailVerificationHtmlBody(verifyUrl, notOwnedUrl),
        textBody = createEmailVerificationTextBody(verifyUrl, notOwnedUrl)
    )

    if (response.errorCode == 406) {
        dao.bouncedEmail.createBouncedEmail(email, "postmark 406")
        dao.star.setEmailStatus(email, EmailStatus.Bounced)
        return@tryOutcome bouncedProblem
    }
    if (response.errorCode != 0) return@tryOutcome Problem("There was an internal error.")

    transaction {
        createTokenOrThrow(starId, verifyToken, email, AuthTokenType.EmailVerification, VerifyEmailInterval)
        createTokenOrThrow(starId, disavowToken, email, AuthTokenType.AccountNotOwned, NotOwnedEmailInterval)
    }

    Ok(Unit)
}

suspend fun DataScope.redeemEmailVerification(token: Token): Outcome<String> = tryOutcome {
    val now = Clock.System.now()
    val hashedToken = hashToken(token)
    val authToken = dao.authToken.readToken(hashedToken, AuthTokenType.EmailVerification)
        ?: return@tryOutcome Problem("Unable to find the request.")

    val account = dao.star.readAccount(authToken.starId) ?: error("account not found")
    if (account.email != authToken.email) return@tryOutcome Problem("The email for this account has changed.")
    val expiredProblem = Problem("This request has expired, please try again.")
    if (authToken.consumedAt != null) {
        if (account.emailStatus == EmailStatus.Verified) return@tryOutcome Ok("Good news! This email has already been verified.")
        else return@tryOutcome expiredProblem
    }
    if (authToken.expiresAt < now) return@tryOutcome expiredProblem

    transaction {
        var updated = dao.authToken.consumeToken(authToken.tokenId, now)
        if (updated != 1) error("unexpected token update count: $updated")
        updated = dao.star.setEmailStatus(authToken.email, EmailStatus.Verified)
        if (updated != 1) error("unexpected email update count: $updated")
    }

    Ok("Success! This email has been verified.")
}

val VerifyEmailInterval = 1.days
val NotOwnedEmailInterval = 2.days

private fun createEmailVerificationHtmlBody(verifyUrl: String, notOwnedUrl: String) = createHTML().html {
    head {
        title("Verify your email")
    }
    body {
        h1 { +"Almost there" }
        p {
            +"Please click the following link to confirm your email address on Streetlight."
        }
        a(href = verifyUrl) {
            +"Verify email"
        }
        p {
            +"This link expires in 24 hours."
        }
        p {
            +"If you didn't sign up for Streetlight, you can remove this address."
        }
        a(href = notOwnedUrl) {
            +"This isn't my account"
        }
    }
}

private fun createEmailVerificationTextBody(verifyUrl: String, disavowUrl: String) = """
Please visit the following link to confirm your email address on Streetlight. It expires in 24 hours.

$verifyUrl

If you didn't sign up for Streetlight, you can remove this address here:

$disavowUrl
""".trimIndent()