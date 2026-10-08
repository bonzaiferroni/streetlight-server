package streetlight.server.db.services

import klutch.db.model.CallerId
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.notInSubQuery
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.CityId
import streetlight.model.data.Entity
import streetlight.model.data.EventLocation
import streetlight.model.data.Location
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.eventLocationQuery
import streetlight.server.db.tables.locationQuery
import streetlight.server.db.tables.toEventLocation
import streetlight.server.db.tables.toLocation
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * The next event at each location in [cityId], and the locations without one, as seen by [callerId].
 *
 * The next event starts after [eventsStartingAfter] when given.
 */
fun cityEntityQuery(
    cityId: CityId,
    callerId: CallerId?,
    eventsStartingAfter: Instant? = null,
): List<Entity> {
    val nextEventIds = EventTable.select(EventTable.id)
        .apply { eventsStartingAfter?.let { andWhere { EventTable.startsAt.greater(it) } } }
        .withDistinctOn(EventTable.locationId)
        .orderBy(
            EventTable.locationId to SortOrder.ASC,
            EventTable.startsAt to SortOrder.ASC,
            EventTable.id to SortOrder.ASC,
        )
    val events = eventLocationQuery(callerId)
        .where { LocationTable.cityId.eq(cityId) and EventTable.id.inSubQuery(nextEventIds) }
        .map { it.toEventLocation() }
    val locationsWithEvents = EventTable.select(EventTable.locationId)
        .apply { eventsStartingAfter?.let { andWhere { EventTable.startsAt.greater(it) } } }
    val locations = locationQuery(callerId)
        .where { LocationTable.cityId.eq(cityId) and LocationTable.id.notInSubQuery(locationsWithEvents) }
        .map { it.toLocation() }
    return events + locations
}

val Entity.cityRecordId: Uuid get() = when (this) {
    is Location -> locationId.value
    is EventLocation -> eventId.value
    else -> error("not a city entity: $this")
}
