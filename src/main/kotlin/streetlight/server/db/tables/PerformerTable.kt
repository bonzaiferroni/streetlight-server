package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import streetlight.model.data.Performer
import streetlight.model.data.PerformerId
import streetlight.server.utils.toRecordId

object PerformerTable : UuidTable("performer") {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE)
    val venmo = text("venmo")
    val stageName = text("stage_name")
}

fun ResultRow.toSpark() = Performer(
    performerId = toRecordId<PerformerId>(PerformerTable.id),
    starId = toRecordId(PerformerTable.starId),
    venmo = this[PerformerTable.venmo],
    stageName = this[PerformerTable.stageName],
)

// Updaters
fun UpdateBuilder<*>.createPerformer(performer: Performer) {
    this[PerformerTable.id] = performer.performerId.value
    this[PerformerTable.starId] = performer.starId.value
    updatePerformer(performer)
}

fun UpdateBuilder<*>.updatePerformer(performer: Performer) {
    this[PerformerTable.venmo] = performer.venmo
    this[PerformerTable.stageName] = performer.stageName
}
