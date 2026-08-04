package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Origin

object OriginTable: IdTable<String>("origin") {
    override val id = varchar("origin_id", 253).entityId()
    override val primaryKey = PrimaryKey(id)
    val robotsTxt = text("robots_txt").nullable()

    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

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