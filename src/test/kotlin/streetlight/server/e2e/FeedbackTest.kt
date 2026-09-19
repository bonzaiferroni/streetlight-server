package streetlight.server.e2e

import kampfire.api.toMarkdown
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import streetlight.model.data.Platform
import streetlight.model.ui.Screen
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("e2e")
class FeedbackTest : BrowserTest() {

    @Test
    fun `a guest sends feedback and is told it arrived`() {
        val text = "The map is lovely but the filters are hard to find."

        openScreen(Screen.Feedback)
        page.openTab("Feedback")

        page.writeIn("feedback", text)
        page.clickButton("Send")

        page.awaitMessage("Feedback Sent.")

        val feedback = assertNotNull(latestFeedbackOrNull(), "the feedback should be stored")
        assertEquals(text.toMarkdown(), feedback.text)
        assertEquals(Platform.Web, feedback.platform)
        assertTrue(feedback.isPrivate, "feedback should be private unless the sender says otherwise")
        assertNull(feedback.username, "a guest leaves no username")
        assertEquals(1, feedbackCount(), "one send should leave one row")
    }
}
