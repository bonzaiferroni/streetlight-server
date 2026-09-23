package streetlight.server.integration

import klutch.db.model.CallerId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import streetlight.model.data.EventEdit
import streetlight.model.data.GalaxyId
import streetlight.model.data.Location
import streetlight.model.data.PostEdit
import streetlight.model.data.PostType
import streetlight.model.data.StarId
import streetlight.server.DatabaseTest
import streetlight.server.TestServer
import streetlight.server.plugins.TableDaemon
import streetlight.server.registerStar
import streetlight.server.seedGalaxy
import streetlight.server.seedLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class UpcomingEventCountTest : DatabaseTest() {

    @Test
    fun `only scheduled events that have not started are counted`() = runTest {
        with(server) {
            val scoutId = registerStar()
            val location = seedLocation(scoutId)
            val galaxyId = seedGalaxy(scoutId)
            seedEventPost(scoutId, location, galaxyId, "Upcoming Show", LocalDate(2030, 3, 14))
            seedEventPost(scoutId, location, galaxyId, "Past Show", LocalDate(2020, 3, 14))
            seedEventPost(scoutId, location, galaxyId, "Unscheduled Show", date = null)

            TableDaemon(dao).aggregate(Clock.System.now())

            assertCounts(location, galaxyId, 1)
        }
    }

    @Test
    fun `an event that has started since the last pass leaves the counts`() = runTest {
        with(server) {
            val scoutId = registerStar()
            val location = seedLocation(scoutId)
            val galaxyId = seedGalaxy(scoutId)
            seedEventPost(scoutId, location, galaxyId, "Upcoming Show", LocalDate(2030, 3, 14))
            seedEventPost(scoutId, location, galaxyId, "Past Show", LocalDate(2020, 3, 14))

            TableDaemon(dao).aggregate(Instant.parse("2019-01-01T00:00:00Z"))
            assertCounts(location, galaxyId, 2)

            TableDaemon(dao).aggregate(Clock.System.now())
            assertCounts(location, galaxyId, 1)
        }
    }

    private suspend fun TestServer.seedEventPost(
        scoutId: StarId,
        location: Location,
        galaxyId: GalaxyId,
        title: String,
        date: LocalDate?,
    ) {
        val callerId = CallerId(scoutId.value)
        val edit = EventEdit(
            title = title,
            locationId = location.locationId,
            date = date,
            startTime = LocalTime(19, 0),
            timeZoneId = "America/Denver",
        )
        val event = dao.event.createEvent(callerId, edit) ?: error("event was not created: $title")
        dao.post.create(PostEdit(null, galaxyId, PostType.Event, event.eventId.value), callerId)
            ?: error("post was not created: $title")
    }

    private suspend fun TestServer.assertCounts(location: Location, galaxyId: GalaxyId, expected: Int) {
        val locationCount = dao.location.readLocation(location.locationId, null)?.eventCount
        val cityCount = dao.city.readCity(location.cityId!!)?.eventCount
        val galaxyCount = dao.galaxy.readGalaxy(galaxyId, null)?.eventCount
        assertEquals(expected, locationCount, "the location should count its upcoming events")
        assertEquals(expected, cityCount, "the city should count the upcoming events of its locations")
        assertEquals(expected, galaxyCount, "the galaxy should count the upcoming events posted to it")
    }
}
