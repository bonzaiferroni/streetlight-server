package streetlight.server.db.tables

import klutch.db.tables.UserTable
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.Performer
import streetlight.model.data.PerformerId
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toUserId

object PerformerTable : UUIDTable("performer") {
    val userId = reference("user_id", UserTable, ReferenceOption.CASCADE)
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
