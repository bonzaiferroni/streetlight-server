package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Lean
import streetlight.model.data.Mark
import streetlight.server.utils.toRecordId

object MarkAspect {
    val GalaxyMarkColumns = listOf(MarkTable.id, GalaxyMarkTable.lean, MarkTable.name)

    fun queryGalaxyMarks(withGalaxyId: Boolean = false) = GalaxyMarkTable
        .join(MarkTable, JoinType.LEFT, GalaxyMarkTable.markId, MarkTable.id)
        .select(if (withGalaxyId) GalaxyMarkColumns + GalaxyMarkTable.galaxyId else GalaxyMarkColumns)
        .orderBy(GalaxyMarkTable.lean, SortOrder.DESC)

    fun queryPostMarks() = PostTable
        .join(GalaxyMarkTable, JoinType.LEFT, PostTable.galaxyId, GalaxyMarkTable.galaxyId)
        .join(MarkTable, JoinType.LEFT, GalaxyMarkTable.markId, MarkTable.id)
        .select(GalaxyMarkColumns)
        .orderBy(GalaxyMarkTable.lean, SortOrder.DESC)
}

fun ResultRow.toGalaxyMark() = toMark(this[GalaxyMarkTable.lean])

fun ResultRow.toMark(lean: Lean) = Mark(
    markId = this[MarkTable.id].toRecordId(),
    lean = lean,
    name = this[MarkTable.name]
)