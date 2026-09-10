package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.GalaxyMark
import streetlight.server.utils.toRecordId

object MarkAspect {
    val GalaxyMarkColumns = listOf(GalaxyMarkTable.id, GalaxyMarkTable.lean, GalaxyMarkTable.name)

    fun queryGalaxyMarks(withGalaxyId: Boolean = false) = GalaxyMarkTable
        .select(if (withGalaxyId) GalaxyMarkColumns + GalaxyMarkTable.galaxyId else GalaxyMarkColumns)
        .orderBy(GalaxyMarkTable.lean, SortOrder.ASC)

    fun queryPostMarks() = PostTable
        .join(GalaxyMarkTable, JoinType.LEFT, PostTable.galaxyId, GalaxyMarkTable.galaxyId)
        .select(GalaxyMarkColumns)
        .orderBy(GalaxyMarkTable.lean, SortOrder.ASC)
}

fun ResultRow.toGalaxyMark() = GalaxyMark(
    markId = this[GalaxyMarkTable.id].toRecordId(),
    lean = this[GalaxyMarkTable.lean],
    name = this[GalaxyMarkTable.name]
)