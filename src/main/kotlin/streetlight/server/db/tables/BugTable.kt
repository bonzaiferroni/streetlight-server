package streetlight.server.db.tables

import kampfire.api.toMarkdown
import kampfire.api.toUsername
import klutch.db.SyncValueTrigger
import klutch.db.model.CallerId
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.*
import streetlight.model.ui.Screen
import kotlin.time.Clock
import kotlin.uuid.Uuid

object BugTable: UuidTable("bug") {
    val starId = reference("star_id", StarTable, ReferenceOption.SET_NULL).nullable().index()
    val buildId = text("build_id").nullable()
    val screen = enumeration<Screen>("screen").nullable()
    val path = text("path").nullable()
    val description = text("description")
    val platform = enumeration<Platform>("platform")
    val deviceAgent = text("device_agent").nullable()
    val status = enumeration<BugStatus>("status")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}


fun ResultRow.toBug() = Bug(
    bugId = BugId(this[BugTable.id].value),
    description = this[BugTable.description].toMarkdown(),
    platform = this[BugTable.platform],
    status = this[BugTable.status],
    updatedAt = this[BugTable.updatedAt],
    createdAt = this[BugTable.createdAt],
)

fun UpdateBuilder<*>.createBug(bug: BugEdit, callerId: CallerId?, buildId: String) {
    this[BugTable.id] = Uuid.random()
    this[BugTable.starId] = callerId?.value
    this[BugTable.buildId] = buildId
    this[BugTable.status] = BugStatus.Open
    this[BugTable.createdAt] = Clock.System.now()
    updateBug(bug)
}

fun UpdateBuilder<*>.updateBug(bug: BugEdit) {
    this[BugTable.description] = bug.description.value
    this[BugTable.platform] = bug.platform
    this[BugTable.screen] = bug.screen
    this[BugTable.path] = bug.path
    this[BugTable.deviceAgent] = bug.deviceAgent
    this[BugTable.updatedAt] = Clock.System.now()
}
