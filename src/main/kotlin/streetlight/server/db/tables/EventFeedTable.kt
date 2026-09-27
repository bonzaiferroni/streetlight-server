package streetlight.server.db.tables

import kampfire.model.toUrl
import klutch.db.point
import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.GeneralEventFeed
import streetlight.server.utils.toRecordId

/** The pages listing local events at many locations, with when each was last read. */
object EventFeedTable: UuidTable("event_feed") {
    val name = text("name")
    val url = text("url")
    val geoPoint = point("geo_point")
    val timezoneId = text("timezone_id")
    val checkedAt = timestamp("checked_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

fun ResultRow.toGeneralEventFeed() = GeneralEventFeed(
    eventFeedId = toRecordId(EventFeedTable.id),
    name = this[EventFeedTable.name],
    url = this[EventFeedTable.url].toUrl(),
    geoPoint = this[EventFeedTable.geoPoint].toGeoPoint(),
    timeZoneId = this[EventFeedTable.timezoneId],
    checkedAt = this[EventFeedTable.checkedAt],
    createdAt = this[EventFeedTable.createdAt],
)
