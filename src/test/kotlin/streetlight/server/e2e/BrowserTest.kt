package streetlight.server.e2e

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.Cookie
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import klutch.db.model.Session
import klutch.server.SESSION_COOKIE_NAME
import klutch.server.provide
import koala.html.AppScreen
import kotlinx.coroutines.runBlocking
import streetlight.server.DatabaseTest
import streetlight.server.db.services.StarSessionService
import streetlight.server.streetlightModule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class BrowserTest : DatabaseTest() {

    companion object {
        private val isHeaded = System.getenv("E2E_HEADED")?.toBoolean() ?: false

        private val playwright: Playwright = Playwright.create()

        val browser: Browser = playwright.chromium().launch(
            BrowserType.LaunchOptions().setHeadless(!isHeaded)
        )
    }

    private lateinit var engine: EmbeddedServer<*, *>
    private lateinit var context: BrowserContext

    protected lateinit var page: Page
    protected var port: Int = 0

    @BeforeTest
    fun startBrowserScenario() {
        engine = embeddedServer(CIO, port = 0) {
            streetlightModule(
                server = server,
                session = server.provide<StarSessionService>(),
                withMetrics = false,
                withDatabase = false,
                withTransit = false,
            )
        }.start(wait = false)

        port = runBlocking { engine.engine.resolvedConnectors().first().port }

        context = browser.newContext(
            Browser.NewContextOptions().setViewportSize(1280, 900)
        )
        page = context.newPage()
    }

    @AfterTest
    fun stopBrowserScenario() {
        context.close()
        engine.stop(0, 0)
    }

    protected fun openScreen(screen: AppScreen) {
        page.navigate(urlOf(screen))
    }

    protected fun urlOf(screen: AppScreen) =
        "http://localhost:$port/" + screen.pathBase.trimStart('/')

    protected fun signIn(session: Session) {
        context.addCookies(
            listOf(
                Cookie(SESSION_COOKIE_NAME, session.token.value)
                    .setDomain("localhost")
                    .setPath("/")
            )
        )
    }
}
