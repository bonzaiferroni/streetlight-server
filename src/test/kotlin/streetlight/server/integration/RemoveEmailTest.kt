package streetlight.server.integration

import kampfire.api.Password
import kampfire.api.obfuscatePassword
import kampfire.model.AuthProblem
import kampfire.model.PasswordResetRequest
import kotlinx.coroutines.test.runTest
import streetlight.model.data.EmailStatus
import streetlight.model.ui.Screen
import streetlight.server.DatabaseTest
import streetlight.server.TestDefault
import streetlight.server.db.services.StarSessionService
import streetlight.server.db.datascope.redeemPasswordReset
import streetlight.server.db.datascope.removeEmail
import streetlight.server.db.datascope.requestPasswordReset
import streetlight.server.extractToken
import streetlight.server.latestMail
import streetlight.server.registerStar
import streetlight.server.registerVerifiedStar
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RemoveEmailTest: DatabaseTest() {

    @Test
    fun `a user with a verified address removes it with their password`() = runTest {
        with(server) {
            val email = TestDefault.emailAddress
            val starId = registerVerifiedStar(email = email)

            // a reset link is outstanding for that address
            requestPasswordReset(email).toDataOrThrow()
            val resetToken = latestMail(email).extractToken(Screen.PasswordReset)

            removeEmail(starId, TestDefault.password).toDataOrThrow()

            // the address is off the account entirely
            val account = assertNotNull(dao.star.readAccount(starId))
            assertNull(account.email, "the address should be gone")
            assertNull(account.emailStatus, "the status should be cleared with it")

            // the old address was told
            val notice = latestMail(email)
            assertEquals("Email changed", notice.subject)

            // and every link tied to that address died with it
            val refused = redeemPasswordReset(
                PasswordResetRequest(resetToken, Password("nu8!!ReefKnot@2").obfuscatePassword()),
                StarSessionService(),
            ).toProblemOrThrow()
            assertEquals("This reset link has expired. Please request a new one.", refused.message)
        }
    }

    @Test
    fun `a verified address survives a missing or wrong password`() = runTest {
        with(server) {
            val email = TestDefault.emailAddress
            val starId = registerVerifiedStar(email = email)
            val mailBefore = emailRouter.count(email)

            // no password offered
            val missing = removeEmail(starId, null).toProblemOrThrow()
            assertEquals(AuthProblem.PasswordRequired.message, missing.message)

            // a password, but not the right one
            val wrong = removeEmail(starId, Password("wr0ng!!PassWord@1")).toProblemOrThrow()
            assertEquals(AuthProblem.InvalidPassword.message, wrong.message)

            // the address stands, unshaken
            val account = assertNotNull(dao.star.readAccount(starId))
            assertEquals(email, account.email)
            assertEquals(EmailStatus.Verified, account.emailStatus)

            // and no notification went out for a change that never happened
            assertEquals(mailBefore, emailRouter.count(email), "no notice should have been sent")
        }
    }

    @Test
    fun `an unverified address is removed without a password`() = runTest {
        with(server) {
            val email = TestDefault.emailAddress
            val starId = registerStar(email = email)

            val before = assertNotNull(dao.star.readAccount(starId))
            assertEquals(EmailStatus.Unverified, before.emailStatus)

            val mailBefore = emailRouter.count(email)

            removeEmail(starId, null).toDataOrThrow()

            val account = assertNotNull(dao.star.readAccount(starId))
            assertNull(account.email, "the address should be gone")
            assertNull(account.emailStatus)

            // the address was still told, even unverified
            assertEquals(mailBefore + 1, emailRouter.count(email), "a notice should have been sent")
            assertEquals("Email changed", latestMail(email).subject)

            // the password is untouched
            assertNotNull(dao.star.readPasswordHash(starId), "the password should still stand")
        }
    }
}