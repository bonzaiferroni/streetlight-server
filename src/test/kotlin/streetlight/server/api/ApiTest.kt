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
import klutch.db.model.Session
import klutch.server.SESSION_COOKIE_NAME
import klutch.server.provide
import streetlight.server.DatabaseTest
import streetlight.server.db.services.StarSessionService
import streetlight.server.streetlightModule

abstract class ApiTest : DatabaseTest() {

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
