package streetlight.server

import kampfire.api.EmailAddress
import kampfire.api.Password
import kampfire.api.TableUuid
import kampfire.api.Username
import kampfire.api.obfuscatePassword
import kampfire.model.AccountType
import kampfire.model.LoginRequest
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.SignUpRequest
import kampfire.model.Token
import klutch.db.model.Session
import klutch.utils.eq
import koala.html.AppScreen
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import streetlight.model.data.AuthTokenType
import streetlight.model.data.StarId
import streetlight.model.ui.Screen
import streetlight.server.db.services.createRegisteredUser
import streetlight.server.db.services.redeemEmailVerification
import streetlight.server.db.tables.AuthToken
import streetlight.server.db.tables.AuthTokenTable
import streetlight.server.db.tables.SessionTable
import streetlight.server.db.tables.toAuthToken
import streetlight.server.model.Email
import streetlight.server.model.EmailRouter
import streetlight.server.model.ServerScope
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

fun <T> Outcome<T>.toDataOrThrow() = when (this) {
    is Ok -> this.data
    is Problem -> error("Problem: $message")
}

fun <T> Outcome<T>.toProblemOrThrow() = when (this) {
    is Ok -> error("Expected a Problem, got: $data")
    is Problem -> this
}

fun signupRequestOf(
    username: Username = TestDefault.username,
    email: EmailAddress = TestDefault.emailAddress,
    password: Password = TestDefault.password,
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

suspend fun TestServer.registerStar(
    username: Username = Username("dingo99"),
    email: EmailAddress = EmailAddress("dingo99@gmail.com"),
    password: Password = Password("ha0!!wfeFAh08FDaw@"),
): StarId = createRegisteredUser(
    signupRequestOf(username = username, email = email, password = password),
    authorizer,
).toDataOrThrow().let { StarId(it.value) }

suspend fun TestServer.registerVerifiedStar(
    username: Username = TestDefault.username,
    email: EmailAddress = TestDefault.emailAddress,
    password: Password = TestDefault.password,
): StarId {
    val starId = registerStar(username, email, password)
    val token = latestMail(email).extractToken(Screen.VerifyEmail)
    redeemEmailVerification(token).toDataOrThrow()
    return starId
}

fun TestServer.latestMail(email: EmailAddress) = emailRouter.latest(email)

fun EmailRouter.latest(email: EmailAddress): Email =
    assertNotNull(inbox(email).latestOrNull(), "No mail waiting for $email")

fun EmailRouter.count(email: EmailAddress): Int = inbox(email).emails.size

object TestDefault {
    val username = Username("dingo99")
    val emailAddress = EmailAddress("dingo99@gmail.com")
    val password = Password("ha0!!wfeFAh08FDaw@")
}

fun TableUuid.toStarId() = StarId(value)

fun sessionCountOf(starId: StarId): Int = transaction {
    SessionTable.selectAll()
        .where { SessionTable.starId.eq(starId) }
        .count()
        .toInt()
}

suspend fun TestServer.loginStar(
    username: Username = TestDefault.username,
    password: Password = TestDefault.password,
    guestToken: Token? = null,
): Session = authorizer.authorize(
    LoginRequest(
        loginIdentity = username.value,
        password = password.obfuscatePassword(),
        isTemp = false,
    ),
    guestToken,
).toDataOrThrow()

fun latestAuthTokenOrNull(starId: StarId, type: AuthTokenType): AuthToken? = transaction {
    AuthTokenTable.selectAll()
        .where { AuthTokenTable.starId.eq(starId) and AuthTokenTable.tokenType.eq(type) }
        .orderBy(AuthTokenTable.createdAt to SortOrder.DESC)
        .limit(1)
        .map { it.toAuthToken() }
        .singleOrNull()
}