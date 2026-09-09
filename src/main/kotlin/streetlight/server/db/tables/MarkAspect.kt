package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Lean
import streetlight.model.data.Mark
import streetlight.server.utils.toRecordId

object MarkAspect {
    val GalaxyColumns = listOf(MarkTable.id, GalaxyMarkTable.lean, GalaxyMarkTable.unmarkId, MarkTable.name)

    fun queryGalaxyMarks(withGalaxyId: Boolean = false) = GalaxyMarkTable
        .join(MarkTable, JoinType.LEFT, GalaxyMarkTable.markId, MarkTable.id)
        .select(if (withGalaxyId) GalaxyColumns + GalaxyMarkTable.galaxyId else GalaxyColumns)
        .orderBy(GalaxyMarkTable.lean, SortOrder.DESC)
}

fun ResultRow.toGalaxyMark() = toMark(this[GalaxyMarkTable.lean])

fun ResultRow.toMark(lean: Lean) = Mark(
    markId = this[MarkTable.id].toRecordId(),
    unmarkId = this[GalaxyMarkTable.unmarkId]?.toRecordId(),
    lean = lean,
    name = this[MarkTable.name]
)