package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Lean
import streetlight.model.data.Mark
import streetlight.server.utils.toRecordId

object MarkAspect {
    val GalaxyColumns = listOf(MarkTable.id, GalaxyMarkTable.lean, MarkTable.name)

    fun queryGalaxy() = GalaxyMarkTable.leftJoin(MarkTable).select(GalaxyColumns)
}

fun ResultRow.toGalaxyMark() = toMark(this[GalaxyMarkTable.lean])

fun ResultRow.toMark(lean: Lean) = Mark(
    markId = this[MarkTable.id].toRecordId(),
    lean = lean,
    name = this[MarkTable.name]
)