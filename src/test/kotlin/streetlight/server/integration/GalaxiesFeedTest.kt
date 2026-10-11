package streetlight.server.integration

import kampfire.api.EmailAddress
import kampfire.api.Username
import klutch.db.model.CallerId
import kotlinx.coroutines.test.runTest
import streetlight.model.data.EntityCursor
import streetlight.model.data.FeedContext
import streetlight.model.data.FeedSource
import streetlight.model.data.FeedType
import streetlight.model.data.Galaxy
import streetlight.model.data.LightEdit
import streetlight.model.data.StarId
import streetlight.model.data.ToggleType
import streetlight.server.DatabaseTest
import streetlight.server.TestServer
import streetlight.server.model.readGalaxiesFeed
import streetlight.server.model.readGalaxyFeedContent
import streetlight.server.registerStar
import streetlight.server.seedGalaxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class GalaxiesFeedTest : DatabaseTest() {

    @Test
    fun `paging the new galaxies returns each galaxy once, newest first`() = runTest {
        with(server) {
            val founderId = registerNamed("alice")
            val expected = (0..EntityCursor.DefaultLimit).map { seedGalaxy(founderId, "galaxy-$it", "Galaxy $it") }.toSet()

            val first = readGalaxiesFeed(FeedType.NewGalaxies, null) ?: error("new galaxies should be a galaxy feed")
            val cursor = first.nextCursor as? EntityCursor.Time ?: error("the first page should lead to a second")
            val second = readGalaxiesFeed(FeedType.NewGalaxies, null, cursor) ?: error("the second page should be read")
            val galaxies = (first.entities + second.entities).map { it as Galaxy }
            val seen = galaxies.map { it.galaxyId }

            assertNull(second.nextCursor, "the second page should be the last")
            assertEquals(seen.size, seen.toSet().size, "no galaxy should appear twice")
            assertEquals(expected, seen.toSet(), "every galaxy should appear")
            val createdTimes = galaxies.map { it.createdAt }
            assertEquals(createdTimes.sortedDescending(), createdTimes, "galaxies should come newest first")
            assertEquals(FeedSource(FeedContext.Galaxies, FeedType.NewGalaxies), first.source, "the feed should name its source")
        }
    }

    @Test
    fun `the top galaxies come by star count, most first`() = runTest {
        with(server) {
            val founderId = registerNamed("alice")
            val quiet = seedGalaxy(founderId, "quiet", "Quiet")
            val busy = seedGalaxy(founderId, "busy", "Busy")
            val middling = seedGalaxy(founderId, "middling", "Middling")
            listOf("bob", "carol").forEach { light(registerNamed(it), busy.value) }
            light(registerNamed("dave"), middling.value)

            val feed = readGalaxiesFeed(FeedType.TopGalaxies, null) ?: error("top galaxies should be a galaxy feed")
            val order = feed.entities.map { (it as Galaxy).galaxyId }

            assertEquals(listOf(busy, middling, quiet), order, "galaxies should come by star count, most first")
        }
    }

    @Test
    fun `a star's galaxies hold only the galaxies they lit, by name in any case`() = runTest {
        with(server) {
            val aliceId = registerNamed("alice")
            val bobId = registerNamed("bob")
            val zebras = seedGalaxy(aliceId, "zebras", "zebras")
            val apples = seedGalaxy(aliceId, "apples", "Apples")
            val bobs = seedGalaxy(bobId, "bobs", "Bobs")
            val mangoes = seedGalaxy(bobId, "mangoes", "mangoes")
            light(aliceId, mangoes.value)

            val feed = readGalaxiesFeed(FeedType.StarGalaxies, CallerId(aliceId.value))
                ?: error("a star's galaxies should be a galaxy feed")
            val order = feed.entities.map { (it as Galaxy).galaxyId }

            assertEquals(listOf(apples, mangoes, zebras), order, "alice's galaxies should come by name, ignoring case")
            assertFalse(bobs in order, "a galaxy alice has not lit should not appear")
        }
    }

    @Test
    fun `searching galaxies matches the name or the tagline in any case`() = runTest {
        with(server) {
            val founderId = registerNamed("alice")
            val byName = seedGalaxy(founderId, "fox-den", "The Fox Den")
            val byTagline = seedGalaxy(founderId, "night-owls", "Night Owls", tagline = "Friends of the FOX")
            seedGalaxy(founderId, "dawn-club", "Dawn Club", tagline = "Early birds")

            // the feed section sends an upcoming cursor whatever its feed
            val cursor = EntityCursor.Upcoming.copy(search = "fox")
            val feed = readGalaxiesFeed(FeedType.NewGalaxies, null, cursor) ?: error("new galaxies should be a galaxy feed")
            val found = feed.entities.map { (it as Galaxy).galaxyId }.toSet()

            assertEquals(setOf(byName, byTagline), found, "only the galaxies naming the fox in name or tagline should appear")
        }
    }

    @Test
    fun `a signed-out user is offered the top and new galaxies, opening on the top`() = runTest {
        with(server) {
            seedGalaxy(registerNamed("alice"), "fox-den", "The Fox Den")

            val content = readGalaxyFeedContent(null)

            assertEquals(listOf(FeedType.TopGalaxies, FeedType.NewGalaxies), content.feed.types, "a signed-out user should not be offered a star's galaxies")
            assertEquals(FeedType.TopGalaxies, content.feed.source?.type, "the feed should open on the top galaxies")
        }
    }

    @Test
    fun `a signed-in star opens the galaxy feed on their own galaxies`() = runTest {
        with(server) {
            val aliceId = registerNamed("alice")
            val galaxyId = seedGalaxy(aliceId, "fox-den", "The Fox Den")

            val content = readGalaxyFeedContent(CallerId(aliceId.value))

            assertEquals(FeedContext.Galaxies.types, content.feed.types, "a star should be offered every galaxy feed")
            assertEquals(FeedType.StarGalaxies, content.feed.source?.type, "the feed should open on the star's galaxies")
            assertEquals(listOf(galaxyId), content.feed.entities.map { (it as Galaxy).galaxyId }, "the star's galaxy should be shown")
        }
    }

    private suspend fun TestServer.registerNamed(name: String) =
        registerStar(Username(name), EmailAddress("$name@gmail.com"))

    private suspend fun TestServer.light(starId: StarId, galaxyId: Uuid) {
        dao.light.editLight(LightEdit(galaxyId, isLit = true, ToggleType.Galaxy), CallerId(starId.value))
    }
}
