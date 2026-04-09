package streetlight.server.db.tables

import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import streetlight.model.data.Performer
import streetlight.model.data.PerformerId
import streetlight.server.utils.toProjectId

object PerformerTable : UUIDTable("performer") {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE)
    val venmo = text("venmo")
    val stageName = text("stage_name")
}

fun ResultRow.toSpark() = Performer(
    performerId = toProjectId<PerformerId>(PerformerTable.id),
    starId = toProjectId(PerformerTable.starId),
    venmo = this[PerformerTable.venmo],
    stageName = this[PerformerTable.stageName],
)

// Updaters
fun UpdateBuilder<*>.writeFull(performer: Performer) {
    this[PerformerTable.id] = performer.performerId.value.toUUID()
    this[PerformerTable.starId] = performer.starId.value.toUUID()
    writeUpdate(performer)
}

fun UpdateBuilder<*>.writeUpdate(performer: Performer) {
    this[PerformerTable.venmo] = performer.venmo
    this[PerformerTable.stageName] = performer.stageName
}
