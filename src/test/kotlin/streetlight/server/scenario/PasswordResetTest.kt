package streetlight.server.scenario

import kampfire.api.EmailAddress
import kampfire.api.Password
import kampfire.api.Username
import kampfire.api.obfuscatePassword
import kampfire.model.PasswordResetRequest
import klutch.server.generateToken
import klutch.server.hashToken
import klutch.utils.eq
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import streetlight.model.data.AuthTokenType
import streetlight.model.ui.Screen
import streetlight.server.DatabaseTest
import streetlight.server.TestDefault
import streetlight.server.buildTestServer
import streetlight.server.db.services.StarSessionService
import streetlight.server.db.services.createToken
import streetlight.server.db.services.redeemPasswordReset
import streetlight.server.db.services.requestEmailVerification
import streetlight.server.db.services.requestPasswordReset
import streetlight.server.db.tables.SessionTable
import streetlight.server.db.tables.StarTable
import streetlight.server.extractToken
import streetlight.server.latestAuthTokenOrNull
import streetlight.server.latestMail
import streetlight.server.loginStar
import streetlight.server.model.TestEmailClient
import streetlight.server.registerStar
import streetlight.server.registerVerifiedStar
import streetlight.server.sessionCountOf
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class PasswordResetTest: DatabaseTest() {

    @Test
    fun `a sailor resets a forgotten password and sails on`() = runTest {
        with(server) {
            val sessionService = StarSessionService()
            val email = TestDefault.emailAddress
            val newPassword = Password("nu8!!ReefKnot@2")

            val starId = registerVerifiedStar(email = email)
            val oldHash = assertNotNull(dao.star.readPasswordHash(starId))

            loginStar()
            assertEquals(1, sessionCountOf(starId), "the sailor should be aboard")

            requestPasswordReset(email).toDataOrThrow()
            val token = latestMail(email).extractToken(Screen.PasswordReset)

            redeemPasswordReset(
                PasswordResetRequest(token, newPassword.obfuscatePassword()),
                sessionService,
            ).toDataOrThrow()

            // the new password stands, the old one does not
            assertNotEquals(oldHash, assertNotNull(dao.star.readPasswordHash(starId)))

            // every prior session is cast off
            assertEquals(0, sessionCountOf(starId), "old sessions should be cut loose")

            // the token is spent — no second passage on the same ticket
            val spent = assertNotNull(
                dao.authToken.readToken(hashToken(token), AuthTokenType.PasswordReset)
            )
            assertNotNull(spent.consumedAt, "the token should be consumed")

            val replay = redeemPasswordReset(
                PasswordResetRequest(token, newPassword.obfuscatePassword()),
                sessionService,
            ).toProblemOrThrow()
            assertEquals("This reset link has expired. Please request a new one.", replay.message)
        }
    }

    @Test
    fun `a reset link dies when the address it was sent to changes`() = runTest {
        with(server) {
            val sessionService = StarSessionService()
            val oldEmail = TestDefault.emailAddress
            val newEmail = EmailAddress("dingo99@protonmail.com")

            val starId = registerVerifiedStar(email = oldEmail)
            val oldHash = assertNotNull(dao.star.readPasswordHash(starId))

            requestPasswordReset(oldEmail).toDataOrThrow()
            val token = latestMail(oldEmail).extractToken(Screen.PasswordReset)

            // the address moves on before the link is followed
            requestEmailVerification(starId, newEmail)

            val refused = redeemPasswordReset(
                PasswordResetRequest(token, Password("nu8!!ReefKnot@2").obfuscatePassword()),
                sessionService,
            ).toProblemOrThrow()

            assertEquals(
                "The email for this account has changed. Please request a new reset link.",
                refused.message,
            )

            // nothing shifted
            assertEquals(oldHash, assertNotNull(dao.star.readPasswordHash(starId)))
            assertNull(
                assertNotNull(dao.authToken.readToken(hashToken(token), AuthTokenType.PasswordReset)).consumedAt,
                "a refused token should not be spent",
            )
        }
    }

    @Test
    fun `a reset request for an unknown address reveals nothing`() = runTest {
        with(server) {
            val stranger = EmailAddress("nobody@gmail.com")

            registerVerifiedStar(email = TestDefault.emailAddress)

            // no account by that name
            requestPasswordReset(stranger).toDataOrThrow()
            assertEquals(0, emailRouter.count(stranger), "no mail should go to a stranger")

            // a bounced address is treated the same way
            val bounced = EmailAddress("gone@gmail.com")
            val bouncedStar = registerStar(username = Username("ghost77"), email = bounced)
            dao.bouncedEmail.createBouncedEmail(bounced, "postmark 406")

            val mailBefore = emailRouter.count(bounced)
            requestPasswordReset(bounced).toDataOrThrow()
            assertEquals(mailBefore, emailRouter.count(bounced), "no mail should chase a bounced address")

            // and no token was minted for either
            assertNull(latestAuthTokenOrNull(bouncedStar, AuthTokenType.PasswordReset))
        }
    }

    @Test
    fun `a second reset request sinks the first link`() = runTest {
        with(server) {
            val sessionService = StarSessionService()
            val email = TestDefault.emailAddress

            val starId = registerVerifiedStar(email = email)
            val oldHash = assertNotNull(dao.star.readPasswordHash(starId))

            requestPasswordReset(email).toDataOrThrow()
            val firstToken = latestMail(email).extractToken(Screen.PasswordReset)

            requestPasswordReset(email).toDataOrThrow()
            val secondToken = latestMail(email).extractToken(Screen.PasswordReset)
            assertNotEquals(firstToken, secondToken, "each request should mint a fresh token")

            // the older link is dead in the water
            val refused = redeemPasswordReset(
                PasswordResetRequest(firstToken, Password("nu8!!ReefKnot@2").obfuscatePassword()),
                sessionService,
            ).toProblemOrThrow()
            assertEquals("This reset link has expired. Please request a new one.", refused.message)
            assertEquals(oldHash, assertNotNull(dao.star.readPasswordHash(starId)))

            // the newest link still carries the sailor home
            redeemPasswordReset(
                PasswordResetRequest(secondToken, Password("nu8!!ReefKnot@2").obfuscatePassword()),
                sessionService,
            ).toDataOrThrow()
            assertNotEquals(oldHash, assertNotNull(dao.star.readPasswordHash(starId)))
        }
    }

    @Test
    fun `an expired reset token opens nothing`() = runTest {
        with(server) {
            val sessionService = StarSessionService()
            val email = TestDefault.emailAddress

            val starId = registerVerifiedStar(email = email)
            val oldHash = assertNotNull(dao.star.readPasswordHash(starId))

            loginStar()
            assertEquals(1, sessionCountOf(starId), "the sailor should be aboard")

            // the sailor lets the window close
            val staleToken = generateToken()
            createToken(starId, staleToken, email, AuthTokenType.PasswordReset, (-1).minutes)

            val refused = redeemPasswordReset(
                PasswordResetRequest(staleToken, Password("nu8!!ReefKnot@2").obfuscatePassword()),
                sessionService,
            ).toProblemOrThrow()
            assertEquals("This reset link has expired. Please request a new one.", refused.message)

            // nothing shifted: same password, same sessions, token unspent
            assertEquals(oldHash, assertNotNull(dao.star.readPasswordHash(starId)))
            assertEquals(1, sessionCountOf(starId), "an expired link should not cast anyone off")
            assertNull(
                assertNotNull(latestAuthTokenOrNull(starId, AuthTokenType.PasswordReset)).consumedAt,
                "a refused token should not be spent",
            )
        }
    }

    @Test
    fun `a forged token and a feeble password both change nothing`() = runTest {
        with(server) {
            val sessionService = StarSessionService()
            val email = TestDefault.emailAddress

            val starId = registerVerifiedStar(email = email)
            val oldHash = assertNotNull(dao.star.readPasswordHash(starId))

            // a token that was never minted
            val forged = redeemPasswordReset(
                PasswordResetRequest(generateToken(), Password("nu8!!ReefKnot@2").obfuscatePassword()),
                sessionService,
            ).toProblemOrThrow()
            assertEquals("This reset link is not valid. Please request a new one.", forged.message)

            // a true token, but a password too plain to hold
            requestPasswordReset(email).toDataOrThrow()
            val token = latestMail(email).extractToken(Screen.PasswordReset)

            val plain = redeemPasswordReset(
                PasswordResetRequest(token, Password("passwordpassword").obfuscatePassword()),
                sessionService,
            ).toProblemOrThrow()
            assertEquals(
                "Password must have at least 3: uppercase, lowercase, number, symbol",
                plain.message,
            )

            // the good token survives the bad attempt
            assertNull(
                assertNotNull(latestAuthTokenOrNull(starId, AuthTokenType.PasswordReset)).consumedAt,
                "a rejected password should not spend the link",
            )
            assertEquals(oldHash, assertNotNull(dao.star.readPasswordHash(starId)))
        }
    }

    @Test
    fun `a refused delivery mints no token`() = runTest {
        var errorCode = 0
        val flakyServer = buildTestServer(
            emailClient = { TestEmailClient(it, getErrorCode = { errorCode }) },
        )

        with(flakyServer) {
            val email = TestDefault.emailAddress
            val starId = registerVerifiedStar(email = email)

            // the mail ship founders
            errorCode = 406
            requestPasswordReset(email).toDataOrThrow()

            assertNull(
                latestAuthTokenOrNull(starId, AuthTokenType.PasswordReset),
                "no link should exist if none was delivered",
            )
            assertTrue(dao.bouncedEmail.readIsBounced(email), "the bounce should be recorded")
        }
    }
}