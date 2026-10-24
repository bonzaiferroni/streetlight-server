package streetlight.server.service

import kampfire.api.deobfuscatePassword
import kampfire.model.AuthProblem
import kampfire.model.EmailChange
import kampfire.model.Outcome
import klutch.server.verifyPassword
import streetlight.model.data.StarId
import streetlight.model.data.viableEmail
import streetlight.server.db.dao.tryOutcome
import streetlight.server.model.DataScope

/**
 * Sets [change]'s new email on [starId]'s account as unverified and sends its verification.
 *
 * An account that has a viable email must confirm the change with its current password. One without may add an
 * email freely, as it may change its password freely.
 */
suspend fun DataScope.changeEmail(starId: StarId, change: EmailChange): Outcome<Unit> = tryOutcome {
    val account = dao.star.readAccount(starId) ?: error("account not found")
    if (account.viableEmail != null) {
        val password = change.passwordNow?.deobfuscatePassword() ?: return@tryOutcome AuthProblem.PasswordRequired
        val hashedPassword = dao.star.readPasswordHash(starId) ?: error("password not found")
        if (!verifyPassword(password, hashedPassword)) return@tryOutcome AuthProblem.InvalidPassword
    }
    requestEmailVerification(starId, change.newEmail)
}
