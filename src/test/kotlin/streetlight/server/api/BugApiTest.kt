package streetlight.server.api

import kampfire.api.toMarkdown
import streetlight.model.Api
import streetlight.model.data.BugEdit
import streetlight.model.data.BugStatus
import streetlight.model.data.Platform
import streetlight.model.ui.Screen
import streetlight.server.bugCount
import streetlight.server.latestBugRowOrNull
import streetlight.server.toDataOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BugApiTest : ApiTest() {

    @Test
    fun `a signed-out user's bug report is stored`() = runApiTest {
        val edit = BugEdit(
            description = "The filters reset when I go back to the map.".toMarkdown(),
            platform = Platform.Web,
            screen = Screen.Feedback,
            path = "/feedback",
            deviceAgent = "test agent",
        )

        postApi(Api.Bugs.Report, edit).toDataOrThrow()

        val bug = assertNotNull(latestBugRowOrNull(), "the bug should be stored")
        assertEquals(edit.description.value, bug.description)
        assertEquals(Platform.Web, bug.platform)
        assertEquals(Screen.Feedback, bug.screen)
        assertEquals("/feedback", bug.path)
        assertEquals("test agent", bug.deviceAgent)
        assertEquals(BugStatus.Open, bug.status, "a new report starts open")
        assertNull(bug.starId, "a signed-out user leaves no star")
        assertEquals("dev", bug.buildId, "the server stamps the build it is running")
        assertEquals(1, bugCount(), "one report should leave one row")
    }
}
