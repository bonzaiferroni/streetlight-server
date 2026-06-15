package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.HostType

object GalaxyHostTable: Table("galaxy_host") {
    val galaxyId = reference("galaxy_id", GalaxyTable, onDelete = ReferenceOption.CASCADE).index()
    val hostId = reference("host_id", StarTable, onDelete = ReferenceOption.CASCADE).index()
    val hostType = enumeration<HostType>("host_type")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(galaxyId, hostId)
}