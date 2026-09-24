package streetlight.server.api

import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters
import kampfire.api.ActionResult
import kampfire.api.EmailAddress
import streetlight.model.data.EmailStatus
import streetlight.model.ui.AccountLockdownRoute
import streetlight.model.ui.AccountNotOwnedRoute
import streetlight.model.ui.ActionReportRoute
import streetlight.model.ui.Screen
import streetlight.model.ui.TokenRoute
import streetlight.server.TestDefault
import streetlight.server.db.datascope.requestEmailVerification
import streetlight.server.extractToken
import streetlight.server.latestMail
import streetlight.server.registerStar
import streetlight.server.registerVerifiedStar
import streetlight.server.toDataOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TokenPageTest : ApiTest() {

    @Test
    fun `a user locks down their account from the emailed page`() = runApiTest {
        val userEmail = TestDefault.emailAddress
        val starId = server.registerVerifiedStar(email = userEmail)
        with(server) { requestEmailVerification(starId, EmailAddress("blackbeard@gmail.com")).toDataOrThrow() }
        val token = server.latestMail(userEmail).extractToken(Screen.AccountLockdown)

        val response = submitTokenPage(AccountLockdownRoute(token))

        assertEquals(
            ActionReportRoute(ActionResult.Success).toRelativePath(),
            response.headers[HttpHeaders.Location],
            "the user should be sent to the success report",
        )
        val account = assertNotNull(server.dao.star.readAccount(starId))
        assertEquals(userEmail, account.email, "the address should revert")
        assertNull(server.dao.star.readPasswordHash(starId), "the password should be disabled")
    }

    @Test
    fun `a user removes their address from the emailed page`() = runApiTest {
        val email = TestDefault.emailAddress
        val starId = server.registerStar(email = email)
        val token = server.latestMail(email).extractToken(Screen.AccountNotOwned)

        val response = submitTokenPage(AccountNotOwnedRoute(token))

        assertEquals(
            ActionReportRoute(ActionResult.Success).toRelativePath(),
            response.headers[HttpHeaders.Location],
            "the user should be sent to the success report",
        )
        val account = assertNotNull(server.dao.star.readAccount(starId))
        assertNull(account.email, "the address should no longer be attached")
        assertEquals(EmailStatus.NotOwned, account.emailStatus)
    }

    /** Opens the page of an emailed [route] and submits its form as rendered. */
    private suspend fun ApiTestScope.submitTokenPage(route: TokenRoute): HttpResponse {
        val page = http.get(route.toRelativePath()).bodyAsText()
        val action = assertNotNull(
            Regex("""<form[^>]*action="([^"]+)"""").find(page),
            "the page should hold a form",
        ).groupValues[1]
        val token = assertNotNull(
            Regex("""name="token"[^>]*value="([^"]+)"""").find(page),
            "the form should carry the token",
        ).groupValues[1]
        return http.submitForm(action, parameters { append("token", token) })
    }
}
