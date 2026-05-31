package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object EventLightTable: Table("event_light"), StarIdTable {
    val eventId = reference("event_id", EventTable, onDelete = ReferenceOption.CASCADE).index()
    override val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(eventId, starId)
}

object GalaxyLightTable: Table("galaxy_light"), StarIdTable {
    val galaxyId = reference("galaxy_id", GalaxyTable, onDelete = ReferenceOption.CASCADE).index()
    override val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(galaxyId, starId)
}

object PostLightTable: Table("post_light"), StarIdTable {
    val postId = reference("post_id", PostTable, onDelete = ReferenceOption.CASCADE).index()
    override val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(postId, starId)
}

object LocationLightTable: Table("location_light"), StarIdTable {
    val locationId = reference("location_id", LocationTable, onDelete = ReferenceOption.CASCADE).index()
    override val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(locationId, starId)
}