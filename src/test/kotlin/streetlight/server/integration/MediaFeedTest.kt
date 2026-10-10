package streetlight.server.integration

import kampfire.api.EmailAddress
import kampfire.api.Username
import klutch.db.model.CallerId
import kotlinx.coroutines.test.runTest
import streetlight.model.data.EntityCursor
import streetlight.model.data.FeedContext
import streetlight.model.data.FeedType
import streetlight.model.data.FeedSource
import streetlight.model.data.Media
import streetlight.model.data.MediaEdit
import streetlight.model.data.MediaId
import streetlight.model.data.StarId
import streetlight.server.DatabaseTest
import streetlight.server.TestServer
import streetlight.server.model.readMediaFeed
import streetlight.server.registerStar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaFeedTest : DatabaseTest() {

    @Test
    fun `paging the home media feed returns each media once, newest first`() = runTest {
        with(server) {
            val starId = registerStar()
            val expected = (0..EntityCursor.DefaultLimit).map { seedMedia(starId, "Song $it") }.toSet()

            val first = readMediaFeed(FeedContext.Home)
            val cursor = first.nextCursor as? EntityCursor.Time ?: error("the first page should lead to a second")
            val second = readMediaFeed(FeedContext.Home, cursor)
            val media = (first.entities + second.entities).map { it as Media }
            val seen = media.map { it.mediaId }
            val createdTimes = media.map { it.createdAt }

            assertNull(second.nextCursor, "the second page should be the last")
            assertEquals(seen.size, seen.toSet().size, "no media should appear twice")
            assertEquals(expected, seen.toSet(), "every media should appear")
            assertEquals(createdTimes.sortedDescending(), createdTimes, "media should come newest first")
            assertEquals(FeedSource(FeedContext.Home, FeedType.Media), first.source, "the feed should name its source")
        }
    }

    @Test
    fun `a star's media feed holds their media and no one else's`() = runTest {
        with(server) {
            val starId = registerStar(Username("alice"), EmailAddress("alice@example.com"))
            val otherId = registerStar(Username("bob"), EmailAddress("bob@example.com"))
            val expected = setOf(seedMedia(starId, "Alice's Song"), seedMedia(starId, "Alice's Poem"))
            seedMedia(otherId, "Bob's Song")

            val seen = readMediaFeed(FeedContext.Star(Username("alice"))).entities.map { (it as Media).mediaId }

            assertEquals(expected, seen.toSet(), "only the star's own media should appear")
        }
    }

    private suspend fun TestServer.seedMedia(starId: StarId, title: String): MediaId {
        val media = dao.media.create(MediaEdit(title = title), CallerId(starId.value)) ?: error("media was not created: $title")
        return media.mediaId
    }
}
