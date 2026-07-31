package streetlight.server.scenario

import kampfire.api.EmailAddress
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import streetlight.server.DatabaseTest
import streetlight.server.db.services.requestEmailVerification
import streetlight.server.toDataOrThrow

class EmailVerificationTest : DatabaseTest() {
//    @Test
//    fun `a user verifies the address they registered with`() = runTest {
//        with(server) {
//            val email = EmailAddress("dingo99@gmail.com")
//            val starId = registerStar(username = "dingo99", email = email)
//
//            requestEmailVerification(starId, email).toDataOrThrow()
//
//            val mail = emailRouter.inbox(email).latest()
//            val token = mail.extractToken(VerifyEmailRoute)
//
//            verifyEmail(token).toDataOrThrow()
//
//            val account = dao.star.readAccount(starId)
//            assertEquals(EmailStatus.Verified, account?.emailStatus)
//        }
//    }
}