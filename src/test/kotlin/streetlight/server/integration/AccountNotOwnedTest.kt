package streetlight.server.integration

import kampfire.api.EmailAddress
import kampfire.api.Password
import kampfire.api.obfuscatePassword
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
import streetlight.server.db.datascope.redeemAccountNotOwned
import streetlight.server.db.datascope.redeemEmailVerification
import streetlight.server.db.datascope.redeemPasswordReset
import streetlight.server.db.datascope.requestEmailVerification
import streetlight.server.db.datascope.requestPasswordReset
import streetlight.server.extractToken
import streetlight.server.latestMail
import streetlight.server.registerStar
import streetlight.server.registerVerifiedStar
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class AccountNotOwnedTest : DatabaseTest() {

    @Test
    fun `a user disavows an address someone else registered`() = runTest {
        with(server) {
            val email = TestDefault.emailAddress
            val starId = registerStar(email = email)

            // one mail carries both doors: verify, and disavow
            val mail = latestMail(email)
            val disavowToken = mail.extractToken(Screen.AccountNotOwned)
            val verifyToken = mail.extractToken(Screen.VerifyEmail)
            assertNotEquals(disavowToken, verifyToken, "the two links should not share a token")

            redeemAccountNotOwned(disavowToken).toDataOrThrow()

            val account = dao.star.readAccount(starId)
            assertEquals(EmailStatus.NotOwned, account?.emailStatus)
            assertNull(account?.email, "the address should no longer be attached")

            // the verification link in that same mail must now be dead
            redeemEmailVerification(verifyToken).toProblemOrThrow()

            // a second walk reports success and changes nothing
            redeemAccountNotOwned(disavowToken).toDataOrThrow()

            val after = dao.star.readAccount(starId)
            assertEquals(EmailStatus.NotOwned, after?.emailStatus)
            assertNull(after?.email)
        }
    }

    @Test
    fun `an expired disavow link leaves the address attached`() = runTest {
        with(server) {
            val email = TestDefault.emailAddress
            val starId = registerStar(email = email)

            val staleToken = generateToken()
            createToken(starId, staleToken, email, AuthTokenType.AccountNotOwned, (-1).days)

            val refused = redeemAccountNotOwned(staleToken).toProblemOrThrow()
            assertEquals(
                "This link has expired. Contact us at ${appEmail.support} to remove your address.",
                refused.message,
            )

            // the address still stands with the account
            val account = dao.star.readAccount(starId)
            assertEquals(email, account?.email)
            assertEquals(EmailStatus.Unverified, account?.emailStatus)
        }
    }

    @Test
    fun `a disavow link cannot strip an address the account no longer holds`() = runTest {
        with(server) {
            val oldEmail = TestDefault.emailAddress
            val newEmail = EmailAddress("dingo99@proton.me")

            val starId = registerStar(email = oldEmail)
            val disavowToken = latestMail(oldEmail).extractToken(Screen.AccountNotOwned)

            // the account moves to a new address before the link is walked
            requestEmailVerification(starId, newEmail).toDataOrThrow()

            // the stale link reports success but says nothing of the change
            redeemAccountNotOwned(disavowToken).toDataOrThrow()

            // and the new address is untouched
            val account = dao.star.readAccount(starId)
            assertEquals(newEmail, account?.email, "the new address should still stand")
            assertNotEquals(EmailStatus.NotOwned, account?.emailStatus)

            // the new address's own verification link still works
            redeemEmailVerification(
                latestMail(newEmail).extractToken(Screen.VerifyEmail)
            ).toDataOrThrow()
            assertEquals(EmailStatus.Verified, dao.star.readAccount(starId)?.emailStatus)
        }
    }

    @Test
    fun `disavowal sweeps every token tied to that address`() = runTest {
        with(server) {
            val email = TestDefault.emailAddress
            val starId = registerVerifiedStar(email = email)

            // a reset link is outstanding for the address
            requestPasswordReset(email).toDataOrThrow()
            val resetToken = latestMail(email).extractToken(Screen.PasswordReset)

            // and a fresh disavow link is minted
            val disavowToken = generateToken()
            createToken(starId, disavowToken, email, AuthTokenType.AccountNotOwned, 1.days)

            redeemAccountNotOwned(disavowToken).toDataOrThrow()
            assertNull(dao.star.readAccount(starId)?.email)

            // the reset link died with the address
            val refused = redeemPasswordReset(
                PasswordResetRequest(resetToken, Password("nu8!!ReefKnot@2").obfuscatePassword()),
                StarSessionService(),
            ).toProblemOrThrow()
            assertEquals("This reset link has expired. Please request a new one.", refused.message)
        }
    }

    @Test
    fun `a forged disavow token is refused`() = runTest {
        with(server) {
            val email = TestDefault.emailAddress
            val starId = registerStar(email = email)

            val refused = redeemAccountNotOwned(generateToken()).toProblemOrThrow()
            assertEquals("This link is not valid.", refused.message)

            // the address still stands
            val account = dao.star.readAccount(starId)
            assertEquals(email, account?.email)
            assertEquals(EmailStatus.Unverified, account?.emailStatus)
        }
    }
}