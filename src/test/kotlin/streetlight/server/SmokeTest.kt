package streetlight.server

import kampfire.api.EmailAddress
import kampfire.api.Password
import kampfire.api.Username
import kampfire.api.obfuscatePassword
import kampfire.model.AccountType
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.SignUpRequest
import kampfire.model.Token
import kampfire.model.UserRole
import klutch.server.Authorizer
import koala.html.AppScreen
import kotlinx.coroutines.test.runTest
import streetlight.model.data.StarId
import streetlight.server.db.services.StarSessionService
import streetlight.server.db.services.StarTableDao
import streetlight.server.model.Email
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SmokeTest : DatabaseTest() {
    @Test
    fun `a star persists and is found again`() = runTest {
        val authorizer = Authorizer(StarSessionService())
        val dao = StarTableDao()
        val outcome = authorizer.createRegisteredUser(SignUpRequest(
            username = Username("dingo99"),
            password = Password("Hunter22!").obfuscatePassword(),
            email = EmailAddress("dingo99@gmail.com"),
            accountType = AccountType.Registered,
            stayLoggedIn = true,
        ), setOf(UserRole.User))
        val starId = StarId(outcome.toDataOrThrow().value)
        val found = dao.readAccount(starId)
        assertEquals(starId, found?.starId)
    }
}

fun <T> Outcome<T>.toDataOrThrow() = when (this) {
    is Ok -> this.data
    is Problem -> error("Problem: $message")
}

fun <T> Outcome<T>.toProblemOrThrow() = when (this) {
    is Ok -> error("Expected a Problem, got: $data")
    is Problem -> this
}

fun signupRequestOf(
    username: Username = Username("dingo99"),
    password: Password = Password("ha0!!wfeFAh08FDaw@"),
    email: EmailAddress = EmailAddress("dingo99@gmail.com"),
    accountType: AccountType = AccountType.Registered,
    stayLoggedIn: Boolean = true
) = SignUpRequest(
    username = username,
    password = password.obfuscatePassword(),
    email = email,
    accountType = accountType,
    stayLoggedIn = stayLoggedIn,
)

fun Email.extractToken(screen: AppScreen): Token {
    val routePrefix = screen.pathBase
    val pattern = Regex("""${Regex.escape(routePrefix)}/([A-Za-z0-9_-]+)""")
    val inText = pattern.find(textBody)?.groupValues?.get(1)
    val inHtml = pattern.find(htmlBody)?.groupValues?.get(1)

    assertNotNull(inText, "No $routePrefix link in the text body")
    assertNotNull(inHtml, "No $routePrefix link in the html body")
    assertEquals(inText, inHtml, "Text and html bodies carry different tokens")

    return Token(inText)
}