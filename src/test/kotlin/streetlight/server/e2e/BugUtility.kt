package streetlight.server.e2e

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import streetlight.model.data.BugStatus
import streetlight.model.data.Platform
import streetlight.model.data.StarId
import streetlight.model.ui.Screen
import streetlight.server.db.tables.BugTable

data class BugRow(
    val starId: StarId?,
    val buildId: String?,
    val screen: Screen?,
    val path: String?,
    val description: String,
    val platform: Platform,
    val deviceAgent: String?,
    val status: BugStatus,
)

private fun ResultRow.toBugRow() = BugRow(
    starId = this[BugTable.starId]?.value?.let(::StarId),
    buildId = this[BugTable.buildId],
    screen = this[BugTable.screen],
    path = this[BugTable.path],
    description = this[BugTable.description],
    platform = this[BugTable.platform],
    deviceAgent = this[BugTable.deviceAgent],
    status = this[BugTable.status],
)

fun latestBugRowOrNull(): BugRow? = transaction {
    BugTable.selectAll()
        .orderBy(BugTable.createdAt to SortOrder.DESC)
        .limit(1)
        .map { it.toBugRow() }
        .singleOrNull()
}

fun bugCount(): Int = transaction {
    BugTable.selectAll().count().toInt()
}
