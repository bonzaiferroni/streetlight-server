package streetlight.server.api

import kampfire.api.EmailAddress
import kampfire.api.Username
import klutch.utils.eq
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import streetlight.model.Api
import streetlight.model.data.EventEdit
import streetlight.model.data.EventId
import streetlight.model.data.LocationId
import streetlight.model.data.StarId
import streetlight.server.db.tables.EventTable
import streetlight.server.registerAdmin
import streetlight.server.seedLocation
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventApiTest : ApiTest() {

    @Test
    fun `a signed-out user cannot create an event`() = runApiTest {
        val starId = registerUser("alice")
        val location = server.seedLocation(starId)

        postApi(Api.Events.CreateEvent, eventEdit(location.locationId)).toProblemOrThrow()

        assertEquals(0, eventCount(), "a signed-out user should leave no event behind")
    }

    @Test
    fun `a user who creates an event as its host becomes the host`() = runApiTest {
        val starId = registerUser("alice")
        val location = server.seedLocation(starId)
        signInAs("alice")

        val event = postApi(Api.Events.CreateEvent, eventEdit(location.locationId, isHost = true)).toDataOrThrow()

        assertEquals(Username("alice"), event.host, "the event should name its host")
        assertEquals(starId, hostOf(event.eventId), "the host should be stored")
        assertEquals(1, eventCount(), "one create should leave one event")
    }

    @Test
    fun `an event created without claiming to host it has no host`() = runApiTest {
        val starId = registerUser("alice")
        val location = server.seedLocation(starId)
        signInAs("alice")

        val event = postApi(Api.Events.CreateEvent, eventEdit(location.locationId)).toDataOrThrow()

        assertNull(hostOf(event.eventId), "an unclaimed event should have no host")
        assertEquals(1, eventCount(), "one create should leave one event")
    }

    @Test
    fun `an event with the same location, time and title is refused`() = runApiTest {
        val starId = registerUser("alice")
        val location = server.seedLocation(starId)
        signInAs("alice")
        postApi(Api.Events.CreateEvent, eventEdit(location.locationId)).toDataOrThrow()

        registerUser("bob")
        signInAs("bob")
        val problem = postApi(Api.Events.CreateEvent, eventEdit(location.locationId)).toProblemOrThrow()

        assertEquals("Event already exists", problem.message)
        assertEquals(1, eventCount(), "the duplicate should not be stored")
    }

    @Test
    fun `any signed-in user can update an event that has no host`() = runApiTest {
        val starId = registerUser("alice")
        val location = server.seedLocation(starId)
        signInAs("alice")
        val event = postApi(Api.Events.CreateEvent, eventEdit(location.locationId)).toDataOrThrow()

        registerUser("bob")
        signInAs("bob")
        val edit = eventEdit(location.locationId, title = "Bob's Night").copy(eventId = event.eventId)
        postApi(Api.Events.UpdateEvent, edit).toDataOrThrow()

        assertEquals("Bob's Night", titleOf(event.eventId), "an unclaimed event should accept the update")
    }

    @Test
    fun `only the host can update an event that has one`() = runApiTest {
        val aliceId = registerUser("alice")
        val location = server.seedLocation(aliceId)
        signInAs("alice")
        val event = postApi(Api.Events.CreateEvent, eventEdit(location.locationId, isHost = true)).toDataOrThrow()

        registerUser("bob")
        signInAs("bob")
        val edit = eventEdit(location.locationId, title = "Bob's Night").copy(eventId = event.eventId)
        postApi(Api.Events.UpdateEvent, edit).toProblemOrThrow()
        assertEquals(TITLE, titleOf(event.eventId), "another user should not retitle a hosted event")

        signInAs("alice")
        postApi(Api.Events.UpdateEvent, edit.copy(title = "Alice's Night")).toDataOrThrow()
        assertEquals("Alice's Night", titleOf(event.eventId), "the host should retitle the event")
    }

    @Test
    fun `only the host of an event can delete it`() = runApiTest {
        val starId = registerUser("alice")
        val location = server.seedLocation(starId)
        signInAs("alice")
        val event = postApi(Api.Events.CreateEvent, eventEdit(location.locationId, isHost = true)).toDataOrThrow()

        registerUser("bob")
        signInAs("bob")
        val deletedByOther = deleteApi(Api.Events.Delete, event.eventId).toDataOrThrow()
        assertEquals(false, deletedByOther, "another user should not delete the event")
        assertEquals(TITLE, titleOf(event.eventId), "the event should remain")

        signInAs("alice")
        val deletedByScout = deleteApi(Api.Events.Delete, event.eventId).toDataOrThrow()
        assertEquals(true, deletedByScout, "the host should delete the event")
        assertNull(titleOf(event.eventId), "the event should be gone")
    }

    @Test
    fun `an admin can delete an event that has a host`() = runApiTest {
        val starId = registerUser("alice")
        val location = server.seedLocation(starId)
        signInAs("alice")
        val event = postApi(Api.Events.CreateEvent, eventEdit(location.locationId, isHost = true)).toDataOrThrow()

        server.registerAdmin(Username("root"), EmailAddress("root@gmail.com"))
        signInAs("root")
        val deleted = deleteApi(Api.Events.Delete, event.eventId).toDataOrThrow()

        assertEquals(true, deleted, "an admin should delete a hosted event")
        assertNull(titleOf(event.eventId), "the event should be gone")
    }

    @Test
    fun `only an admin can delete an event that has no host`() = runApiTest {
        val starId = registerUser("alice")
        val location = server.seedLocation(starId)
        signInAs("alice")
        val event = postApi(Api.Events.CreateEvent, eventEdit(location.locationId)).toDataOrThrow()

        val deletedByCreator = deleteApi(Api.Events.Delete, event.eventId).toDataOrThrow()
        assertEquals(false, deletedByCreator, "a non-admin should not delete an unclaimed event")
        assertEquals(TITLE, titleOf(event.eventId), "the event should remain")

        server.registerAdmin(Username("root"), EmailAddress("root@gmail.com"))
        signInAs("root")
        val deletedByAdmin = deleteApi(Api.Events.Delete, event.eventId).toDataOrThrow()
        assertEquals(true, deletedByAdmin, "an admin should delete an unclaimed event")
        assertNull(titleOf(event.eventId), "the event should be gone")
    }

    private fun eventEdit(locationId: LocationId, title: String = TITLE, isHost: Boolean? = null) = EventEdit(
        title = title,
        locationId = locationId,
        date = LocalDate(2030, 3, 14),
        startTime = LocalTime(19, 0),
        endTime = LocalTime(21, 0),
        timeZoneId = "America/Denver",
        isHost = isHost,
    )

    private fun hostOf(eventId: EventId) = transaction {
        EventTable.selectAll().where { EventTable.id.eq(eventId) }.single()[EventTable.hostId]?.value?.let(::StarId)
    }

    private fun eventCount() = transaction { EventTable.selectAll().count().toInt() }

    private fun titleOf(eventId: EventId) = transaction {
        EventTable.selectAll().where { EventTable.id.eq(eventId) }.singleOrNull()?.get(EventTable.title)
    }
}

private const val TITLE = "Open Mic Night"
