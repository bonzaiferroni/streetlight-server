package streetlight.server.db.tables

import kampfire.model.normalize
import kampfire.model.toUrl
import klutch.db.point
import klutch.utils.toGeoPoint
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.EventLead
import streetlight.model.data.GeneralEventFeed
import streetlight.model.data.Lead
import streetlight.model.data.LeadType
import streetlight.model.data.LocationLead
import streetlight.server.utils.toRecordId

/**
 * The leads the crawler is given by hand or by users, each of a [LeadType], with who submitted it and from which
 * galaxy, and when it was last read.
 */
object LeadTable: UuidTable("lead") {
    val leadType = enumeration<LeadType>("lead_type")
    val name = text("name").nullable()
    val url = text("url")
    val geoPoint = point("geo_point").nullable()
    val timezoneId = text("timezone_id").nullable()
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val galaxyId = reference("galaxy_id", GalaxyTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val checkedAt = timestamp("checked_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

/** The lead of this row, of the kind its type names, its url normalized. */
fun ResultRow.toLead(): Lead = when (val leadType = this[LeadTable.leadType]) {
    LeadType.EventFeed -> GeneralEventFeed(
        leadId = toRecordId(LeadTable.id),
        name = requireNotNull(this[LeadTable.name]) { "event feed lead without a name" },
        initialUrl = this[LeadTable.url].toUrl().normalize(),
        geoPoint = requireNotNull(this[LeadTable.geoPoint]) { "event feed lead without a geo point" }.toGeoPoint(),
        timeZoneId = requireNotNull(this[LeadTable.timezoneId]) { "event feed lead without a time zone" },
        checkedAt = this[LeadTable.checkedAt],
        createdAt = this[LeadTable.createdAt],
    )
    LeadType.Location -> LocationLead(
        leadId = toRecordId(LeadTable.id),
        initialUrl = this[LeadTable.url].toUrl().normalize(),
        checkedAt = this[LeadTable.checkedAt],
        createdAt = this[LeadTable.createdAt],
    )
    LeadType.Event -> EventLead(
        leadId = toRecordId(LeadTable.id),
        initialUrl = this[LeadTable.url].toUrl().normalize(),
        checkedAt = this[LeadTable.checkedAt],
        createdAt = this[LeadTable.createdAt],
    )
    LeadType.EventPage -> error("event page leads are not stored")
}
