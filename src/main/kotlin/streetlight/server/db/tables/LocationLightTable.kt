package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object LocationLightTable: Table("location_light") {
    val locationId = reference("location_id", LocationTable, onDelete = ReferenceOption.CASCADE).index()
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(locationId, starId)
}