package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.db.inRect
import klutch.utils.eq
import klutch.utils.inList
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import streetlight.model.data.Entity
import streetlight.model.data.EntityCursor
import streetlight.model.data.EventGroup
import streetlight.model.data.LocationId
import streetlight.model.data.MapQuery
import streetlight.model.data.SortDirection
import streetlight.server.db.tables.EventGroupAspect
import streetlight.server.db.tables.EventLocationAspect
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationAspect
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.joinEventStar
import streetlight.server.db.tables.joinWith
import streetlight.server.db.tables.joinLocationStar
import streetlight.server.db.tables.selectWith
import streetlight.server.db.tables.toEventGroup
import streetlight.server.db.tables.toEventLocation
import streetlight.server.db.tables.toLocation
import streetlight.server.db.tables.selectEventStar
import streetlight.server.db.tables.selectLocationStar
import kotlin.time.Clock

class EarthDao: DbService() {
    suspend fun readBoundedEntities(query: MapQuery): List<EventGroup> = dbQuery {
        LocationTable.joinWith(EventGroupAspect)
            .selectWith(EventGroupAspect.columns)
            .whereInView(query)
            .whereAfterScore(query.cursor)
            .orderByScore(query.cursor)
            .limit(EntityCursor.MapLimit)
            .map { it.toEventGroup() }
    }

    suspend fun readLocationId(
        locationId: LocationId,
        callerId: CallerId?,
    ): List<Entity> = dbQuery {
        LocationTable.joinWith(EventTable) {
            EventTable.startsAt.greaterEq(Clock.System.now())
        }
            .joinEventStar(callerId)
            .joinLocationStar(callerId)
            .selectWith(EventLocationAspect.columns, LocationAspect.columns) {
                selectEventStar(callerId)
                selectLocationStar(callerId)
            }
            .where { LocationTable.id.eq(locationId) }
            .orderBy(EventTable.startsAt, SortOrder.ASC_NULLS_LAST)
            .limit(EntityCursor.DefaultLimit)
            .map {
                if (it.getOrNull(EventTable.id) != null) {
                    it.toEventLocation()
                } else {
                    it.toLocation()
                }
            }
    }
}

/**
 * Filters locations joined with their events to the rows after [cursor].
 *
 * Event rows come first, then rows of locations without events.
 */
private fun Query.whereAfterEvent(cursor: EntityCursor.Time): Query {
    val recordId = cursor.recordId ?: return this
    val recordAt = cursor.recordAt
    val isLocationOnly = EventTable.id.isNull()
    val afterLocation = when (cursor.direction) {
        SortDirection.Descending -> LocationTable.id.less(recordId)
        SortDirection.Ascending -> LocationTable.id.greater(recordId)
    }
    if (recordAt == null) return andWhere { isLocationOnly and afterLocation }
    val afterEvent = when (cursor.direction) {
        SortDirection.Descending -> EventTable.startsAt.less(recordAt) or
            (EventTable.startsAt.eq(recordAt) and EventTable.id.less(recordId))
        SortDirection.Ascending -> EventTable.startsAt.greater(recordAt) or
            (EventTable.startsAt.eq(recordAt) and EventTable.id.greater(recordId))
    }
    return andWhere { isLocationOnly or afterEvent }
}

/** Orders locations joined with their events as [whereAfterEvent] pages them. */
private fun Query.orderByEvent(cursor: EntityCursor.Time): Query {
    val order = when (cursor.direction) {
        SortDirection.Descending -> SortOrder.DESC_NULLS_LAST
        SortDirection.Ascending -> SortOrder.ASC_NULLS_LAST
    }
    return orderBy(EventTable.startsAt to order, EventTable.id to order, LocationTable.id to order)
}

/** Filters locations to those in the view of [query], leaving out the areas it has seen. */
private fun Query.whereInView(query: MapQuery): Query = andWhere {
    query.seen.orEmpty().fold(LocationTable.geoPoint.inRect(query.view)) { op, rect ->
        op and not(LocationTable.geoPoint.inRect(rect))
    }
}

/**
 * Filters locations to those after [cursor] by map priority.
 *
 * Locations with a priority come first, then those without.
 */
private fun Query.whereAfterScore(cursor: EntityCursor.Score): Query {
    val recordId = cursor.recordId ?: return this
    val score = cursor.score
    val isUnscored = LocationTable.mapPriority.isNull()
    val afterLocation = when (cursor.direction) {
        SortDirection.Descending -> LocationTable.id.less(recordId)
        SortDirection.Ascending -> LocationTable.id.greater(recordId)
    }
    if (score == null) return andWhere { isUnscored and afterLocation }
    val afterScore = when (cursor.direction) {
        SortDirection.Descending -> LocationTable.mapPriority.less(score) or
            (LocationTable.mapPriority.eq(score) and afterLocation)
        SortDirection.Ascending -> LocationTable.mapPriority.greater(score) or
            (LocationTable.mapPriority.eq(score) and afterLocation)
    }
    return andWhere { isUnscored or afterScore }
}

/** Orders locations as [whereAfterScore] pages them. */
private fun Query.orderByScore(cursor: EntityCursor.Score): Query {
    val order = when (cursor.direction) {
        SortDirection.Descending -> SortOrder.DESC_NULLS_LAST
        SortDirection.Ascending -> SortOrder.ASC_NULLS_LAST
    }
    return orderBy(LocationTable.mapPriority to order, LocationTable.id to order)
}
