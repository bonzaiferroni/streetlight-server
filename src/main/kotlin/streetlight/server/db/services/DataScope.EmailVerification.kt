package streetlight.server.db.services

import kampfire.api.EmailAddress
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Token
import klutch.server.generateToken
import klutch.server.hashToken
import klutch.server.provide
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
import streetlight.model.data.StarId
import streetlight.model.ui.AccountLockdownRoute
import streetlight.model.ui.AccountNotOwnedRoute
import streetlight.model.ui.VerifyEmailRoute
import streetlight.server.model.EmailClient
import streetlight.server.model.DataScope
import streetlight.server.model.Email
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

suspend fun DataScope.requestEmailVerification(starId: StarId, email: EmailAddress): Outcome<Unit> = tryOutcome {
    val account = dao.star.readAccount(starId) ?: return@tryOutcome Problem("Account not found.")
    val emailNow = account.email
    val bouncedProblem = Problem("This address can't receive mail. Try a different one.")
    val isBounced = dao.bouncedEmail.readIsBounced(email)
    if (email == emailNow) {
        if (account.emailStatus == EmailStatus.Verified) return@tryOutcome Problem("This email is already verified.")
        if (account.emailStatus == EmailStatus.Bounced || account.emailStatus == EmailStatus.NotOwned)
            return@tryOutcome bouncedProblem
        if (isBounced) {
            dao.star.setEmailStatus(email, EmailStatus.Bounced)
            return@tryOutcome bouncedProblem
        }
    }

    if (isBounced) return@tryOutcome bouncedProblem

    val holder = dao.star.readAccount(email)
    if (holder != null && holder.starId != starId)
        return@tryOutcome Problem("This email is already in use.")

    if (emailNow != null && email != emailNow) {
        if (!sendCredentialChangeNotification(starId, emailNow)) error("Unable to notify previous address")
    }

    if (dao.star.setEmail(starId, email, EmailStatus.Unverified) != 1)
        error("email was not set")

    val verifyToken = generateToken()
    val disavowToken = generateToken()
    val verifyUrl = VerifyEmailRoute(verifyToken).toAbsolutePath()
    val notOwnedUrl = AccountNotOwnedRoute(disavowToken).toAbsolutePath()

    val response = client.postmark.sendEmail(
        Email(
            from = appEmail.primary,
            to = email,
            subject = "Email verification",
            htmlBody = createEmailVerificationHtmlBody(verifyUrl, notOwnedUrl),
            textBody = createEmailVerificationTextBody(verifyUrl, notOwnedUrl)
        )
    )

    recordEmailBounced(response, email)
    if (response.errorCode != 0) return@tryOutcome Problem("There was an internal error.")

    transaction {
        createToken(starId, verifyToken, email, AuthTokenType.EmailVerification, VerifyEmailInterval)
        createToken(starId, disavowToken, email, AuthTokenType.AccountNotOwned, NotOwnedEmailInterval)
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

suspend fun DataScope.sendCredentialChangeNotification(starId: StarId, email: EmailAddress): Boolean {
    val token = generateToken()
    val url = AccountLockdownRoute(token).toAbsolutePath()

    val response = client.postmark.sendEmail(
        Email(
            from = appEmail.support,
            to = email,
            subject = "Email changed",
            htmlBody = createEmailChangedHtmlBody(url),
            textBody = createEmailChangedTextBody(url)
        )
    )

    recordEmailBounced(response, email)
    if (response.errorCode != 0) return false

    createToken(starId, token, email, AuthTokenType.AccountLockdown, AccountLockdownInterval, consumePrior = false)
    return true
}

val VerifyEmailInterval = 1.days
val NotOwnedEmailInterval = 2.days

private fun createEmailVerificationHtmlBody(
    verifyUrl: String,
    notOwnedUrl: String,
) = createHTML().html {
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

private fun createEmailChangedHtmlBody(url: String) = createHTML().html {
    head {
        title("Streetlight Email Changed")
    }
    body {
        p {
            +"The email for your Streetlight account has changed."
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

private fun createEmailChangedTextBody(url: String) = """
The email for your Streetlight account has changed.

If this was you, no action is needed.

If it wasn't, secure your account now. This will sign out every device and let you set a new password:

$url
""".trimIndent()