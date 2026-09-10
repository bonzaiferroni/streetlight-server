package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Lean

//object MarkTable: UuidTable("mark") {
//    val name = text("name").uniqueIndex()
//    val createdAt = timestamp("created_at")
//}

object GalaxyMarkTable: UuidTable("galaxy_mark") {
    val galaxyId = reference("galaxy_id", GalaxyTable, ReferenceOption.CASCADE).index()
//    val markId = reference("mark_id", MarkTable, ReferenceOption.CASCADE).nullable().index()
    val name = text("name")
    val lean = enumeration<Lean>("lean")
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(galaxyId, name)
    }
}

object PostMarkTable: Table("post_mark"), StarIdTable {
    val postId = reference("post_id", PostTable, ReferenceOption.CASCADE).index()
    override val starId = reference("star_id", StarTable, ReferenceOption.CASCADE).index()
    val galaxyMarkId = reference("galaxy_mark_id", GalaxyMarkTable, ReferenceOption.CASCADE)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(postId, starId, galaxyMarkId)
}

object PostMarkCountTable: Table("post_mark_count") {
    val postId = reference("post_id", PostTable, ReferenceOption.CASCADE)
    val galaxyMarkId = reference("galaxy_mark_id", GalaxyMarkTable, ReferenceOption.CASCADE)
    val count = integer("count").default(0)

    override val primaryKey = PrimaryKey(postId, galaxyMarkId)

    init {
        index(false, galaxyMarkId, count, postId)
    }
}