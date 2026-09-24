package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** The events each star has lit. */
object EventStarTable: Table("event_star"), StarIdTable {
    val eventId = reference("event_id", EventTable, onDelete = ReferenceOption.CASCADE).index()
    override val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(eventId, starId)
}

/** The galaxies each star has lit. */
object GalaxyStarTable: Table("galaxy_star"), StarIdTable {
    val galaxyId = reference("galaxy_id", GalaxyTable, onDelete = ReferenceOption.CASCADE).index()
    override val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(galaxyId, starId)
}

/** The locations each star has lit. */
object LocationStarTable: Table("location_star"), StarIdTable {
    val locationId = reference("location_id", LocationTable, onDelete = ReferenceOption.CASCADE).index()
    override val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(locationId, starId)
}

/** The comments each star has lit. */
object CommentStarTable: Table("comment_star") {
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val commentId = reference("comment_id", CommentTable, onDelete = ReferenceOption.CASCADE).index()

    override val primaryKey = PrimaryKey(starId, commentId)
}