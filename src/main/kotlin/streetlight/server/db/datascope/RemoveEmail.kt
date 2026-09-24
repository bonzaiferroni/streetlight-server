package streetlight.server.db.datascope

import kampfire.api.Password
import kampfire.model.AuthProblem
import kampfire.model.Ok
import kampfire.model.Outcome
import klutch.server.verifyPassword
import streetlight.model.data.EmailStatus
import streetlight.model.data.StarId
import streetlight.server.db.services.tryOutcome
import streetlight.server.model.DataScope

/** Removes the star's email, with their [password] when it is verified, and tells the address. */
suspend fun DataScope.removeEmail(starId: StarId, password: Password?): Outcome<Unit> = tryOutcome {
    val account = dao.star.readAccount(starId) ?: error("account not found")
    val email = account.email ?: error("email not found")
    when (account.emailStatus) {
        EmailStatus.Verified -> {
            if (password == null) return@tryOutcome AuthProblem.PasswordRequired
            val hashedPassword = dao.star.readPasswordHash(starId) ?: error("password not found")
            if (!verifyPassword(password, hashedPassword)) return@tryOutcome AuthProblem.InvalidPassword
            sendCredentialChangeNotification(starId, email)
        }
        EmailStatus.Unverified, null -> {
            sendCredentialChangeNotification(starId, email)
        }
        EmailStatus.Bounced, EmailStatus.NotOwned -> { }
    }

    transaction {
        dao.authToken.consumeAllTokensForEmail(starId, email)
        dao.star.setEmail(starId, null, null)
    }

    Ok(Unit)
}