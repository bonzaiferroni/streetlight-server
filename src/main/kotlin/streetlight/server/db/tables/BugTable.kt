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
import kotlin.time.Clock
import kotlin.uuid.Uuid

object BugTable: UuidTable("bug") {
    val starId = reference("star_id", StarTable, ReferenceOption.SET_NULL).nullable().index()
    val username = text("username").nullable()
    val text = text("text")
    val platform = enumeration<Platform>("platform")
    val deviceAgent = text("device_agent").nullable()
    val status = enumeration<BugStatus>("status")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

val bugUsernameSync = SyncValueTrigger(BugTable.starId, BugTable.username, StarTable, StarTable.username)

fun ResultRow.toBug() = Bug(
    bugId = BugId(this[BugTable.id].value),
    username = this[BugTable.username]?.toUsername(),
    text = this[BugTable.text].toMarkdown(),
    platform = this[BugTable.platform],
    status = this[BugTable.status],
    updatedAt = this[BugTable.updatedAt],
    createdAt = this[BugTable.createdAt],
)

fun UpdateBuilder<*>.createRecord(bug: BugEdit, callerId: CallerId?) {
    this[BugTable.id] = Uuid.random()
    this[BugTable.starId] = callerId?.value
    this[BugTable.status] = BugStatus.Open
    this[BugTable.createdAt] = Clock.System.now()
    writeUpdate(bug)
}

fun UpdateBuilder<*>.writeUpdate(bug: BugEdit) {
    this[BugTable.text] = bug.text.value
    this[BugTable.platform] = bug.platform
    this[BugTable.deviceAgent] = bug.deviceAgent
    this[BugTable.updatedAt] = Clock.System.now()
}
