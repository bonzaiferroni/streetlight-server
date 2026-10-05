package streetlight.server.db.tables

import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.EventGroup
import streetlight.model.data.EventTag
import streetlight.model.data.LocationId

/** A location with its next event, as an [EventGroup] reads it. */
object EventGroupAspect {
    /** The next event of each location, or none when it has no upcoming event. */
    val nextEvent = EventTable.select(EventTable.id, EventTable.title, EventTable.image, EventTable.tags, EventTable.startsAt)
        .where { EventTable.locationId.eq(LocationTable.id) and EventTable.startsAt.greaterEq(CurrentTimestamp) }
        .orderBy(EventTable.startsAt to SortOrder.ASC, EventTable.id to SortOrder.ASC)
        .limit(1)
        .alias("next_event")

    val eventTitle = nextEvent[EventTable.title]
    val eventImage = nextEvent[EventTable.image]
    val eventTags = nextEvent[EventTable.tags]
    val eventStartsAt = nextEvent[EventTable.startsAt]

    val columns = listOf(
        LocationTable.id,
        LocationTable.name,
        LocationTable.address,
        LocationTable.image,
        LocationTable.geoPoint,
        LocationTable.eventCount,
        LocationTable.mapPriority,
        eventTitle,
        eventImage,
        eventTags,
        eventStartsAt,
    )
}

/** Left joins each location's next event, as [EventGroupAspect] reads it. */
fun LocationTable.joinWith(aspect: EventGroupAspect) =
    join(aspect.nextEvent, JoinType.LEFT, lateral = true) { Op.TRUE }

fun ResultRow.toEventGroup() = EventGroup(
    locationId = LocationId(this[LocationTable.id].value),
    label = getOrNull(EventGroupAspect.eventTitle) ?: this[LocationTable.name] ?: this[LocationTable.address] ?: "",
    image = this[EventGroupAspect.eventImage] ?: this[LocationTable.image],
    geoPoint = this[LocationTable.geoPoint].toGeoPoint(),
    eventCount = this[LocationTable.eventCount],
    tag = this[EventGroupAspect.eventTags]?.firstOrNull()?.let { EventTag.entries[it] },
    startsAt = this[EventGroupAspect.eventStartsAt],
    mapPriority = this[LocationTable.mapPriority],
)
