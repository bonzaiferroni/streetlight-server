package streetlight.server.e2e

import kampfire.api.toMarkdown
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import streetlight.model.data.FeedbackEdit
import streetlight.model.data.FeedbackType
import streetlight.model.data.Platform
import streetlight.model.ui.Screen
import streetlight.server.TestDefault
import streetlight.server.loginStar
import streetlight.server.registerAdmin
import streetlight.server.registerStar
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("e2e")
class FeedbackTest : BrowserTest() {

    @Test
    fun `a guest sends feedback and is told it arrived`() {
        val text = "The map is lovely but the filters are hard to find."

        openFeedback()
        sendFeedback(text)

        val feedback = assertNotNull(latestFeedbackOrNull(), "the feedback should be stored")
        assertEquals(text.toMarkdown(), feedback.text)
        assertEquals(Platform.Web, feedback.platform)
        assertTrue(feedback.isPrivate, "feedback should be private unless the sender says otherwise")
        assertNull(feedback.username, "a guest leaves no username")
        assertEquals(1, feedbackCount(), "one send should leave one row")

        val deviceAgent = assertNotNull(latestDeviceAgentOrNull(), "the browser should report itself")
        assertTrue(
            deviceAgent.contains("viewport=1280x900"),
            "the device agent should carry the viewport, was: $deviceAgent",
        )
    }

    @Test
    fun `public feedback is shown in the public feed`() {
        val text = "The events map is the best part of this."

        openFeedback()
        page.chooseIn("sharing", "Share Publicly")
        page.chooseIn("type", FeedbackType.Issue.label)
        sendFeedback(text)

        page.awaitText(text)

        val feedback = assertNotNull(latestFeedbackOrNull(), "the feedback should be stored")
        assertFalse(feedback.isPrivate, "the sender chose to share publicly")
        assertEquals(FeedbackType.Issue, feedback.feedbackType)
    }

    @Test
    fun `private feedback is kept out of the public feed`() {
        val anchor = "A public note that anyone may read."
        val secret = "A private note meant only for the crew."

        seedFeedback(anchor, isPrivate = false)

        openFeedback()
        sendFeedback(secret)

        page.awaitText(anchor)
        page.awaitNoText(secret)

        val feedback = assertNotNull(latestFeedbackOrNull(), "the feedback should be stored")
        assertEquals(secret.toMarkdown(), feedback.text)
        assertTrue(feedback.isPrivate, "the sender left the default sharing")
        assertEquals(2, feedbackCount(), "the seeded note and the sent one")
    }

    @Test
    fun `a signed-in sailor's feedback carries their username`() {
        val text = "Signing in was easy enough."

        val session = runBlocking {
            server.registerStar()
            server.loginStar()
        }
        signIn(session)

        openFeedback()
        sendFeedback(text)

        val feedback = assertNotNull(latestFeedbackOrNull(), "the feedback should be stored")
        assertEquals(TestDefault.username, feedback.username, "the row should carry the sender")

        page.awaitNoText(text)
    }

    @Test
    fun `an admin sees private feedback in the feed`() {
        val secret = "A private note meant only for the crew."

        seedFeedback(secret, isPrivate = true)

        val session = runBlocking {
            server.registerAdmin()
            server.loginStar()
        }
        signIn(session)

        openFeedback()
        page.awaitText(secret)
    }

    @Test
    fun `a second note is sent without reopening the page`() {
        val first = "The first thing I noticed was the map."
        val second = "The second thing was the events feed."

        openFeedback()
        sendFeedback(first)
        sendFeedback(second)

        assertEquals(2, feedbackCount(), "both notes should be stored")
        assertEquals(second.toMarkdown(), assertNotNull(latestFeedbackOrNull()).text)
    }

    private fun openFeedback() {
        openScreen(Screen.Feedback)
        page.openTab("Feedback")
    }

    private fun sendFeedback(text: String) {
        page.writeIn("feedback", text)
        page.clickButton("Send")
        page.awaitMessage("Feedback Sent.")
        page.awaitEditorCleared("feedback", text)
    }

    private fun seedFeedback(text: String, isPrivate: Boolean) = runBlocking {
        server.dao.feedback.create(
            FeedbackEdit(
                text = text.toMarkdown(),
                platform = Platform.Web,
                isPrivate = isPrivate,
            ),
            null,
        )
    }
}
