package streetlight.server.e2e

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import streetlight.model.data.BugStatus
import streetlight.model.data.Platform
import streetlight.model.ui.Screen
import streetlight.server.bugCount
import streetlight.server.latestBugRowOrNull
import streetlight.server.loginStar
import streetlight.server.registerStar
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("e2e")
class BugTest : BrowserTest() {

    @Test
    fun `a guest reports a bug and is told it arrived`() {
        val description = "The filters reset when I go back to the map."

        openBugReporter()
        reportBug(description)

        val bug = assertNotNull(latestBugRowOrNull(), "the bug should be stored")
        assertEquals(description, bug.description)
        assertEquals(Platform.Web, bug.platform)
        assertEquals(BugStatus.Open, bug.status, "a new report starts open")
        assertNull(bug.starId, "a guest leaves no star")
        assertEquals("dev", bug.buildId, "the server stamps the build it is running")
        assertNotNull(bug.screen, "the client should report the screen")
        assertNotNull(bug.path, "the client should report the path")
        assertEquals(1, bugCount(), "one send should leave one row")

        val deviceAgent = assertNotNull(bug.deviceAgent, "the browser should report itself")
        assertTrue(
            deviceAgent.contains("viewport=1280x900"),
            "the device agent should carry the viewport, was: $deviceAgent",
        )
    }

    @Test
    fun `a signed-in sailor's bug report carries their star id`() {
        val description = "The sign-in page keeps asking for my password twice."

        val (starId, session) = runBlocking {
            server.registerStar() to server.loginStar()
        }
        signIn(session)

        openBugReporter()
        reportBug(description)

        val bug = assertNotNull(latestBugRowOrNull(), "the bug should be stored")
        assertEquals(starId, bug.starId, "the row should carry the reporter")
        assertEquals(1, bugCount(), "one send should leave one row")
    }

    private fun openBugReporter() {
        openScreen(Screen.Feedback)
        page.openTab("Report a bug")
    }

    private fun reportBug(description: String) {
        page.writeIn("bug", description)
        page.clickButton("Send")
        page.awaitMessage("Bug Reported.")
        page.awaitEditorCleared("bug", description)
    }
}
