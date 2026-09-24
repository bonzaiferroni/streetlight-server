package streetlight.server.integration

import kampfire.api.EmailAddress
import kampfire.api.Slug
import kampfire.api.Username
import kampfire.api.toMarkdown
import klutch.db.model.CallerId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import streetlight.model.data.EventEdit
import streetlight.model.data.EventPost
import streetlight.model.data.GalaxyEdit
import streetlight.model.data.GalaxyId
import streetlight.model.data.LightEdit
import streetlight.model.data.LocationPost
import streetlight.model.data.PostEdit
import streetlight.model.data.PostType
import streetlight.model.data.StarId
import streetlight.model.data.ToggleType
import streetlight.server.DENVER_AREA
import streetlight.server.DatabaseTest
import streetlight.server.TestServer
import streetlight.server.registerStar
import streetlight.server.seedLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class GalaxyFeedTest : DatabaseTest() {

    @Test
    fun `a user reads an event post once, lit only by their own light`() = runTest {
        with(server) {
            val hostId = registerNamed("alice")
            val readerId = registerNamed("dave")
            val galaxyId = seedGalaxy(hostId)
            val location = seedLocation(hostId)
            val edit = EventEdit(
                title = "Fox Den Open Mic",
                locationId = location.locationId,
                date = LocalDate(2030, 3, 14),
                startTime = LocalTime(19, 0),
                timeZoneId = "America/Denver",
            )
            val event = dao.event.createEvent(CallerId(hostId.value), edit) ?: error("event was not created")
            seedPost(hostId, galaxyId, PostType.Event, event.eventId.value)

            lightByOthers(ToggleType.Event, event.eventId.value)

            val unlit = dao.post.readGalaxyPosts(galaxyId, CallerId(readerId.value)).filterIsInstance<EventPost>()
            assertEquals(1, unlit.size, "the post should appear once however many others lit the event")
            assertFalse(unlit.single().event.isLit, "another user's light should not show as the reader's")

            light(readerId, ToggleType.Event, event.eventId.value)

            val lit = dao.post.readGalaxyPosts(galaxyId, CallerId(readerId.value)).filterIsInstance<EventPost>()
            assertEquals(1, lit.size, "the post should still appear once")
            assertTrue(lit.single().event.isLit, "the reader's own light should show")
        }
    }

    @Test
    fun `a user reads a location post once, lit only by their own light`() = runTest {
        with(server) {
            val hostId = registerNamed("alice")
            val readerId = registerNamed("dave")
            val galaxyId = seedGalaxy(hostId)
            val location = seedLocation(hostId)
            seedPost(hostId, galaxyId, PostType.Location, location.locationId.value)

            lightByOthers(ToggleType.Location, location.locationId.value)

            val unlit = dao.post.readGalaxyPosts(galaxyId, CallerId(readerId.value)).filterIsInstance<LocationPost>()
            assertEquals(1, unlit.size, "the post should appear once however many others lit the location")
            assertFalse(unlit.single().location.isLit, "another user's light should not show as the reader's")

            light(readerId, ToggleType.Location, location.locationId.value)

            val lit = dao.post.readGalaxyPosts(galaxyId, CallerId(readerId.value)).filterIsInstance<LocationPost>()
            assertEquals(1, lit.size, "the post should still appear once")
            assertTrue(lit.single().location.isLit, "the reader's own light should show")
        }
    }

    private suspend fun TestServer.registerNamed(name: String) =
        registerStar(Username(name), EmailAddress("$name@gmail.com"))

    private suspend fun TestServer.seedGalaxy(hostId: StarId): GalaxyId {
        val edit = GalaxyEdit(name = "Fox Friends", slug = Slug("fox-friends"), geoRect = DENVER_AREA, marks = emptyList())
        val slug = dao.galaxy.create(edit, CallerId(hostId.value), null)
        return dao.galaxy.readGalaxy(slug, null)?.galaxyId ?: error("galaxy was not created")
    }

    private suspend fun TestServer.seedPost(authorId: StarId, galaxyId: GalaxyId, postType: PostType, recordId: Uuid) {
        val edit = PostEdit(
            postId = null,
            galaxyId = galaxyId,
            postType = postType,
            recordId = recordId,
            title = "A first post",
            text = "Come on down.".toMarkdown(),
        )
        dao.post.create(edit, CallerId(authorId.value)) ?: error("post was not created")
    }

    /** Two users other than the reader light the record, so an unconstrained join would double the post. */
    private suspend fun TestServer.lightByOthers(toggleType: ToggleType, targetId: Uuid) {
        light(registerNamed("bob"), toggleType, targetId)
        light(registerNamed("carol"), toggleType, targetId)
    }

    private suspend fun TestServer.light(starId: StarId, toggleType: ToggleType, targetId: Uuid) {
        dao.light.editLight(LightEdit(targetId, isLit = true, toggleType), CallerId(starId.value))
    }
}
