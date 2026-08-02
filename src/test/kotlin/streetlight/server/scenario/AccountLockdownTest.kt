package streetlight.server.scenario

import kampfire.api.EmailAddress
import kampfire.api.Password
import kampfire.api.obfuscatePassword
import kampfire.model.LoginRequest
import kampfire.model.PasswordResetRequest
import klutch.server.generateToken
import kotlinx.coroutines.test.runTest
import streetlight.model.data.AuthTokenType
import streetlight.model.data.EmailStatus
import streetlight.model.ui.Screen
import streetlight.server.DatabaseTest
import streetlight.server.TestDefault
import streetlight.server.db.services.StarSessionService
import streetlight.server.db.datascope.createToken
import streetlight.server.db.datascope.redeemAccountLockdown
import streetlight.server.db.datascope.redeemEmailVerification
import streetlight.server.db.datascope.redeemPasswordReset
import streetlight.server.db.datascope.requestEmailVerification
import streetlight.server.extractToken
import streetlight.server.latestMail
import streetlight.server.registerVerifiedStar
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class AccountLockdownTest: DatabaseTest() {
    @Test
    fun `a sailor locks out an attacker who moved their address`() = runTest {
        with(server) {
            val sessionService = StarSessionService()
            val sailorEmail = TestDefault.emailAddress
            val attackerEmail = EmailAddress("blackbeard@gmail.com")

            val starId = registerVerifiedStar(email = sailorEmail)

            // the attacker, holding the password, moves the address to their own
            requestEmailVerification(starId, attackerEmail).toDataOrThrow()
            assertEquals(attackerEmail, dao.star.readAccount(starId)?.email)

            // the sailor's address was warned
            val notice = latestMail(sailorEmail)
            assertEquals("Email changed", notice.subject)
            val lockdownToken = notice.extractToken(Screen.AccountLockdown)

            redeemAccountLockdown(lockdownToken, sessionService).toDataOrThrow()

            val account = assertNotNull(dao.star.readAccount(starId))
            val passwordHash = dao.star.readPasswordHash(starId)
            assertEquals(sailorEmail, account.email, "the address should revert")
            assertEquals(EmailStatus.Verified, account.emailStatus)
            assertNull(passwordHash, "the password should be disabled")

            // the sailor is handed a way back in
            latestMail(sailorEmail).extractToken(Screen.PasswordReset)

            // the attacker's verification token is spent
            val attackerToken = latestMail(attackerEmail).extractToken(Screen.VerifyEmail)
            redeemEmailVerification(attackerToken).toProblemOrThrow()
        }
    }

    @Test
    fun `an older lockdown token trumps one the attacker redeems`() = runTest {
        with(server) {
            val sessionService = StarSessionService()
            val sailorEmail = TestDefault.emailAddress
            val firstHop = EmailAddress("blackbeard@gmail.com")
            val secondHop = EmailAddress("blackbeard@proton.me")

            val starId = registerVerifiedStar(email = sailorEmail)

            requestEmailVerification(starId, firstHop).toDataOrThrow()
            val sailorToken = latestMail(sailorEmail).extractToken(Screen.AccountLockdown)

            requestEmailVerification(starId, secondHop).toDataOrThrow()
            val attackerToken = latestMail(firstHop).extractToken(Screen.AccountLockdown)

            // the attacker redeems theirs first
            redeemAccountLockdown(attackerToken, sessionService).toDataOrThrow()
            assertEquals(firstHop, dao.star.readAccount(starId)?.email)

            // the sailor's older token still stands, and wins
            redeemAccountLockdown(sailorToken, sessionService).toDataOrThrow()
            val account = assertNotNull(dao.star.readAccount(starId))
            assertEquals(sailorEmail, account.email)
            assertEquals(EmailStatus.Verified, account.emailStatus)
        }
    }

    @Test
    fun `a locked-out sailor recovers with the reset link`() = runTest {
        with(server) {
            val sessionService = StarSessionService()
            val sailorEmail = TestDefault.emailAddress
            val attackerEmail = EmailAddress("blackbeard@gmail.com")
            val newPassword = Password("aY3!qmZ0vLp8@wRt")
            val oldPassword = TestDefault.password

            val starId = registerVerifiedStar(email = sailorEmail)
            requestEmailVerification(starId, attackerEmail).toDataOrThrow()

            val lockdownToken = latestMail(sailorEmail).extractToken(Screen.AccountLockdown)
            redeemAccountLockdown(lockdownToken, sessionService).toDataOrThrow()
            assertNull(dao.star.readPasswordHash(starId), "the password should be disabled")

            val resetToken = latestMail(sailorEmail).extractToken(Screen.PasswordReset)
            redeemPasswordReset(PasswordResetRequest(resetToken, newPassword.obfuscatePassword()), sessionService).toDataOrThrow()

            assertNotNull(dao.star.readPasswordHash(starId), "the sailor holds a password again")
            assertEquals(sailorEmail, dao.star.readAccount(starId)?.email)

            authorizer.authorize(LoginRequest(sailorEmail.value, false, newPassword.obfuscatePassword()), null).toDataOrThrow()
            // the old password must not let the attacker back in
            val refused = authorizer.authorize(
                LoginRequest(sailorEmail.value, false, oldPassword.obfuscatePassword()), null,
            ).toProblemOrThrow()
            assertEquals("Invalid password", refused.message)
        }
    }

    // td: write a test confirming that a message has been dispatched if the !canRevert block renders a user locked out

    @Test
    fun `an expired lockdown token secures nothing`() = runTest {
        with(server) {
            val sessionService = StarSessionService()
            val sailorEmail = TestDefault.emailAddress
            val attackerEmail = EmailAddress("blackbeard@gmail.com")

            val starId = registerVerifiedStar(email = sailorEmail)
            requestEmailVerification(starId, attackerEmail).toDataOrThrow()
            val mailBefore = emailRouter.count(sailorEmail)

            // the sailor lets the window close
            val staleToken = generateToken()
            createToken(
                starId, staleToken, sailorEmail,
                AuthTokenType.AccountLockdown, (-1).days,
                consumePrior = false,
            )

            val refused = redeemAccountLockdown(staleToken, sessionService).toProblemOrThrow()
            assertEquals(
                "This link has expired. Contact us at ${appEmail.support.value} " +
                        "for help securing your account.",
                refused.message,
            )

            // nothing was secured
            val account = assertNotNull(dao.star.readAccount(starId))
            assertEquals(attackerEmail, account.email, "the address should not revert")
            assertNotNull(dao.star.readPasswordHash(starId), "the password should still stand")
            assertEquals(mailBefore, emailRouter.count(sailorEmail), "no reset should go out")
        }
    }

    @Test
    fun `a second lockdown click changes nothing`() = runTest {
        with(server) {
            val sessionService = StarSessionService()
            val sailorEmail = TestDefault.emailAddress
            val attackerEmail = EmailAddress("blackbeard@gmail.com")
            val newPassword = Password("aY3!qmZ0vLp8@wRt")

            val starId = registerVerifiedStar(email = sailorEmail)
            requestEmailVerification(starId, attackerEmail).toDataOrThrow()

            val lockdownToken = latestMail(sailorEmail).extractToken(Screen.AccountLockdown)
            redeemAccountLockdown(lockdownToken, sessionService).toDataOrThrow()

            val resetToken = latestMail(sailorEmail).extractToken(Screen.PasswordReset)
            val mailSoFar = emailRouter.count(sailorEmail)

            // the alarmed sailor clicks again from another device
            redeemAccountLockdown(lockdownToken, sessionService).toDataOrThrow()
            assertEquals(mailSoFar, emailRouter.count(sailorEmail), "no second reset should go out")

            // the link already sitting open in their inbox still works
            redeemPasswordReset(PasswordResetRequest(resetToken, newPassword.obfuscatePassword()), sessionService)
                .toDataOrThrow()
            assertNotNull(dao.star.readPasswordHash(starId))
        }
    }
}