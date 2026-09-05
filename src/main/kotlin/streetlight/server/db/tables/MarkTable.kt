package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Lean

object MarkTable: UuidTable("mark") {
    val name = text("name").uniqueIndex()
    val createdAt = timestamp("created_at")
}

object GalaxyMarkTable: Table("galaxy_mark") {
    val galaxyId = reference("galaxy_id", GalaxyTable, ReferenceOption.CASCADE)
    val markId = reference("mark_id", MarkTable, ReferenceOption.CASCADE)
    val lean = enumeration<Lean>("lean")

    override val primaryKey = PrimaryKey(galaxyId, markId)
}

object PostMarkTable: Table("post_mark"), StarIdTable {
    val postId = reference("post_id", PostTable, ReferenceOption.CASCADE).index()
    override val starId = reference("star_id", StarTable, ReferenceOption.CASCADE).index()
    val markId = reference("mark_id", MarkTable, ReferenceOption.CASCADE)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(postId, starId, markId)
}