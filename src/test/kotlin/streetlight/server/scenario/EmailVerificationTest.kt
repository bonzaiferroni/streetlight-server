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
import streetlight.server.buildTestServer
import streetlight.server.db.services.createRegisteredUser
import streetlight.server.db.services.createToken
import streetlight.server.db.services.redeemAccountNotOwned
import streetlight.server.db.services.redeemEmailVerification
import streetlight.server.db.services.requestEmailVerification
import streetlight.server.extractToken
import streetlight.server.model.TestEmailClient
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
            val email = EmailAddress("dingo99@gmail.com")
            val starId = createRegisteredUser(signupRequestOf(email = email), authorizer).toDataOrThrow().let {
                StarId(it.value)
            }

            val mail = assertNotNull(emailRouter.inbox(email).latestOrNull(), "No email found")
            val token = mail.extractToken(Screen.VerifyEmail)

            // also check for not-owned token
            mail.extractToken(Screen.AccountNotOwned)

            redeemEmailVerification(token).toDataOrThrow()

            val account = dao.star.readAccount(starId)
            assertEquals(EmailStatus.Verified, account?.emailStatus)
        }
    }

    @Test
    fun `a second verification request consumes the first token`() = runTest {
        with(server) {
            val email = EmailAddress("dingo99@gmail.com")
            val starId = createRegisteredUser(signupRequestOf(email = email), authorizer)
                .toDataOrThrow().let { StarId(it.value) }

            requestEmailVerification(starId, email).toDataOrThrow()

            val inbox = emailRouter.inbox(email)
            inbox.emails.forEach { println("${it.subject} -> ${it.to}") }
            assertEquals(2, inbox.emails.size, "Both requests should have sent mail")

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
    fun `a sailor disavows an address someone else registered`() = runTest {
        with(server) {
            val email = EmailAddress("dingo99@gmail.com")
            val starId = createRegisteredUser(signupRequestOf(email = email), authorizer)
                .toDataOrThrow().let { StarId(it.value) }

            val mail = assertNotNull(emailRouter.inbox(email).latestOrNull())
            val disavowToken = mail.extractToken(Screen.AccountNotOwned)
            val verifyToken = mail.extractToken(Screen.VerifyEmail)

            redeemAccountNotOwned(disavowToken).toDataOrThrow()

            val account = dao.star.readAccount(starId)
            assertEquals(EmailStatus.NotOwned, account?.emailStatus)
            assertNull(account?.email, "The address should no longer be attached")

            // the verification link in that same mail must now be dead
            println(redeemEmailVerification(verifyToken).message)
            redeemEmailVerification(verifyToken).toProblemOrThrow()
        }
    }

    @Test
    fun `a bounced address is recorded and refused thereafter`() = runTest {
        val bouncingServer = buildTestServer(
            emailClient = { TestEmailClient(it, errorCode = 406) },
        )

        with(bouncingServer) {
            val authorizer = provide<Authorizer>()
            val email = EmailAddress("dingo99@gmail.com")
            val starId = createRegisteredUser(signupRequestOf(email = email), authorizer)
                .toDataOrThrow().let { StarId(it.value) }

            assertTrue(dao.bouncedEmail.readIsBounced(email), "The bounce should be recorded")

            val outcome = requestEmailVerification(starId, email).toProblemOrThrow()
            assertEquals("This address can't receive mail. Try a different one.", outcome.message)
            assertEquals(0, emailRouter.inbox(email).emails.size, "No mail should have been attempted")
        }
    }

    @Test
    fun `an address already held by another sailor is refused`() = runTest {
        with(server) {
            val heldEmail = EmailAddress("dingo99@gmail.com")
            createRegisteredUser(signupRequestOf(email = heldEmail), authorizer).toDataOrThrow()

            val interloperEmail = EmailAddress("bingo00@gmail.com")
            val interloperId = createRegisteredUser(
                signupRequestOf(username = Username("bingo00"), email = interloperEmail),
                authorizer,
            ).toDataOrThrow().let { StarId(it.value) }

            val outcome = requestEmailVerification(interloperId, heldEmail).toProblemOrThrow()
            assertEquals("This email is already in use.", outcome.message)

            // the interloper's own address should be untouched
            assertEquals(interloperEmail, dao.star.readAccount(interloperId)?.email)
        }
    }

    @Test
    fun `an expired verification token is refused`() = runTest {
        with(server) {
            val email = EmailAddress("dingo99@gmail.com")
            val starId = createRegisteredUser(signupRequestOf(email = email), authorizer)
                .toDataOrThrow().let { StarId(it.value) }

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
            val oldEmail = EmailAddress("dingo99@gmail.com")
            val newEmail = EmailAddress("dingo99@proton.me")

            val starId = createRegisteredUser(signupRequestOf(email = oldEmail), authorizer)
                .toDataOrThrow().let { StarId(it.value) }

            val verifyToken = assertNotNull(emailRouter.inbox(oldEmail).latestOrNull())
                .extractToken(Screen.VerifyEmail)
            redeemEmailVerification(verifyToken).toDataOrThrow()
            assertEquals(EmailStatus.Verified, dao.star.readAccount(starId)?.emailStatus)

            requestEmailVerification(starId, newEmail).toDataOrThrow()

            // the account now holds the new address, unverified
            val account = dao.star.readAccount(starId)
            assertEquals(newEmail, account?.email)
            assertEquals(EmailStatus.Unverified, account?.emailStatus)

            // the old address was warned, and holds a lockdown token
            val notice = assertNotNull(emailRouter.inbox(oldEmail).latestOrNull())
            assertEquals("Email changed", notice.subject)
            val lockdownToken = notice.extractToken(Screen.AccountLockdown)

            // the new address can complete verification
            val newToken = assertNotNull(emailRouter.inbox(newEmail).latestOrNull())
                .extractToken(Screen.VerifyEmail)
            redeemEmailVerification(newToken).toDataOrThrow()
            assertEquals(EmailStatus.Verified, dao.star.readAccount(starId)?.emailStatus)

            assertEquals(2, emailRouter.inbox(oldEmail).emails.size)
            assertEquals(1, emailRouter.inbox(newEmail).emails.size)
        }
    }
}