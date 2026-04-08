package streetlight.server.db.tables

import klutch.db.tables.BasicUserTable
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import streetlight.model.data.Performer
import streetlight.model.data.PerformerId
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toUserId

object PerformerTable : UUIDTable("performer") {
    val userId = reference("user_id", BasicUserTable, ReferenceOption.CASCADE)
    val venmo = text("venmo")
    val stageName = text("stage_name")
}

fun ResultRow.toSpark() = Performer(
    performerId = toProjectId<PerformerId>(PerformerTable.id),
    userId = toUserId(PerformerTable.userId),
    venmo = this[PerformerTable.venmo],
    stageName = this[PerformerTable.stageName],
)

// Updaters
fun UpdateBuilder<*>.writeFull(performer: Performer) {
    this[PerformerTable.id] = performer.performerId.value.toUUID()
    this[PerformerTable.userId] = performer.userId.value.toUUID()
    writeUpdate(performer)
}

fun UpdateBuilder<*>.writeUpdate(performer: Performer) {
    this[PerformerTable.venmo] = performer.venmo
    this[PerformerTable.stageName] = performer.stageName
}
