package streetlight.server.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kampfire.api.EmailAddress
import kampfire.api.Username
import klutch.db.model.Session
import klutch.server.SESSION_COOKIE_NAME
import klutch.server.provide
import streetlight.model.data.StarId
import streetlight.server.DatabaseTest
import streetlight.server.db.services.StarSessionService
import streetlight.server.loginStar
import streetlight.server.registerStar
import streetlight.server.streetlightModule

abstract class ApiTest : DatabaseTest() {

    protected suspend fun registerUser(name: String): StarId =
        server.registerStar(Username(name), EmailAddress("$name@gmail.com"))

    protected suspend fun ApiTestScope.signInAs(name: String) = signIn(server.loginStar(Username(name)))

    protected fun runApiTest(block: suspend ApiTestScope.() -> Unit) = testApplication {
        application {
            streetlightModule(
                server = server,
                session = server.provide<StarSessionService>(),
            )
        }
        val cookies = AcceptAllCookiesStorage()
        val http = createClient {
            install(ContentNegotiation) { json() }
            install(HttpCookies) { storage = cookies }
        }
        ApiTestScope(http, cookies).block()
    }
}

class ApiTestScope(val http: HttpClient, private val cookies: CookiesStorage) {

    suspend fun signIn(session: Session) {
        cookies.addCookie(
            Url("http://localhost/"),
            Cookie(SESSION_COOKIE_NAME, session.token.value, path = "/"),
        )
    }
}
