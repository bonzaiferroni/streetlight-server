package streetlight.server.scenario

import kampfire.api.EmailAddress
import kampfire.api.Username
import klutch.server.Authorizer
import klutch.server.generateToken
import klutch.server.provide
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import streetlight.model.data.AuthTokenType
import streetlight.model.data.EmailStatus
import streetlight.model.data.StarId
import streetlight.model.ui.Screen
import streetlight.server.DatabaseTest
import streetlight.server.TestDefault
import streetlight.server.buildTestServer
import streetlight.server.db.services.createRegisteredUser
import streetlight.server.db.services.createToken
import streetlight.server.db.services.redeemAccountNotOwned
import streetlight.server.db.services.redeemEmailVerification
import streetlight.server.db.services.requestEmailVerification
import streetlight.server.extractToken
import streetlight.server.latestMail
import streetlight.server.model.TestEmailClient
import streetlight.server.registerStar
import streetlight.server.registerVerifiedStar
import streetlight.server.signupRequestOf
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class EmailVerificationTest : DatabaseTest() {

    @Test
    fun `a sailor verifies the address they registered with`() = runTest {
        with(server) {
            val email = TestDefault.emailAddress
            val starId = registerStar(email = email)

            val token = latestMail(email).extractToken(Screen.VerifyEmail)
            redeemEmailVerification(token).toDataOrThrow()

            assertEquals(EmailStatus.Verified, dao.star.readAccount(starId)?.emailStatus)
        }
    }

    @Test
    fun `a second verification request consumes the first token`() = runTest {
        with(server) {
            val email = TestDefault.emailAddress
            val starId = registerStar(email = email)

            requestEmailVerification(starId, email).toDataOrThrow()
            assertEquals(2, emailRouter.count(email), "both requests should have sent mail")

            val inbox = emailRouter.inbox(email)
            val firstToken = inbox.emails[0].extractToken(Screen.VerifyEmail)
            val secondToken = inbox.emails[1].extractToken(Screen.VerifyEmail)
            assertNotEquals(firstToken, secondToken)

            redeemEmailVerification(firstToken).toProblemOrThrow()
            assertEquals(EmailStatus.Unverified, dao.star.readAccount(starId)?.emailStatus)

            redeemEmailVerification(secondToken).toDataOrThrow()
            assertEquals(EmailStatus.Verified, dao.star.readAccount(starId)?.emailStatus)
        }
    }

    @Test
    fun `a bounced address is recorded and refused thereafter`() = runTest {
        val bouncingServer = buildTestServer(
            emailClient = { TestEmailClient(it, getErrorCode = { 406 }) },
        )

        with(bouncingServer) {
            val email = TestDefault.emailAddress
            val starId = registerStar(email = email)

            assertTrue(dao.bouncedEmail.readIsBounced(email), "the bounce should be recorded")

            val outcome = requestEmailVerification(starId, email).toProblemOrThrow()
            assertEquals("This address can't receive mail. Try a different one.", outcome.message)
            assertEquals(0, emailRouter.count(email), "no mail should have been attempted")
        }
    }

    @Test
    fun `an address already held by another sailor is refused`() = runTest {
        with(server) {
            val heldEmail = TestDefault.emailAddress
            registerStar(email = heldEmail)

            val interloperEmail = EmailAddress("bingo00@gmail.com")
            val interloperId = registerStar(
                username = Username("bingo00"),
                email = interloperEmail,
            )

            val outcome = requestEmailVerification(interloperId, heldEmail).toProblemOrThrow()
            assertEquals("This email is already in use.", outcome.message)

            assertEquals(interloperEmail, dao.star.readAccount(interloperId)?.email)
        }
    }

    @Test
    fun `an expired verification token is refused`() = runTest {
        with(server) {
            val email = TestDefault.emailAddress
            val starId = registerStar(email = email)

            val staleToken = generateToken()
            createToken(starId, staleToken, email, AuthTokenType.EmailVerification, (-1).days)

            val outcome = redeemEmailVerification(staleToken).toProblemOrThrow()
            assertEquals("This request has expired, please try again.", outcome.message)
            assertEquals(EmailStatus.Unverified, dao.star.readAccount(starId)?.emailStatus)
        }
    }

    @Test
    fun `a sailor changes to a new address and the old one is notified`() = runTest {
        with(server) {
            val oldEmail = TestDefault.emailAddress
            val newEmail = EmailAddress("dingo99@proton.me")

            val starId = registerVerifiedStar(email = oldEmail)
            assertEquals(EmailStatus.Verified, dao.star.readAccount(starId)?.emailStatus)

            requestEmailVerification(starId, newEmail).toDataOrThrow()

            // the account now holds the new address, unverified
            val account = dao.star.readAccount(starId)
            assertEquals(newEmail, account?.email)
            assertEquals(EmailStatus.Unverified, account?.emailStatus)

            // the old address was warned, and holds a lockdown token
            val notice = latestMail(oldEmail)
            assertEquals("Email changed", notice.subject)
            notice.extractToken(Screen.AccountLockdown)

            // the new address can complete verification
            redeemEmailVerification(latestMail(newEmail).extractToken(Screen.VerifyEmail)).toDataOrThrow()
            assertEquals(EmailStatus.Verified, dao.star.readAccount(starId)?.emailStatus)

            assertEquals(2, emailRouter.count(oldEmail))
            assertEquals(1, emailRouter.count(newEmail))
        }
    }
}