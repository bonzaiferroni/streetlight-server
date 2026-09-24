package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.FetchMode
import streetlight.model.data.Origin

/** The web origins location data is read from, with how each is fetched and its robots.txt. */
object OriginTable: IdTable<String>("origin") {
    override val id = varchar("origin_id", 253).entityId()
    override val primaryKey = PrimaryKey(id)
    val robotsTxt = text("robots_txt").nullable()
    val fetchMode = enumeration<FetchMode>("fetch_mode").default(FetchMode.Basic)

    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

/** The origins each location reads from. */
object LocationOriginTable: Table("location_origin") {
    val locationId = reference("location_id", LocationTable, ReferenceOption.CASCADE).index()
    val originId = reference("origin_id", OriginTable, ReferenceOption.CASCADE).index()

    override val primaryKey = PrimaryKey(locationId, originId)
}

fun UpdateBuilder<*>.createOrigin(origin: Origin) {
    this[OriginTable.id] = origin.originId.value
    this[OriginTable.createdAt] = origin.createdAt
    updateOrigin(origin)
}

fun UpdateBuilder<*>.updateOrigin(origin: Origin) {
    this[OriginTable.robotsTxt] = origin.robotsTxt
    this[OriginTable.updatedAt] = origin.updatedAt
}