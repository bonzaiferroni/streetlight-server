package streetlight.server.db.services

import kampfire.api.Username
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.ChatId
import streetlight.model.data.ChatPreview
import streetlight.server.db.tables.ChatStarTable
import streetlight.server.db.tables.ChatTable
import streetlight.server.db.tables.StarTable
import kotlin.collections.orEmpty

object ChatPreviewAspect {
    val columns = with(ChatTable) {
        listOf(
            id, subject, lastMessagePreview, lastMessageAt, createdAt,
            ChatStarTable.lastReadAt, ChatStarTable.archivedAt
        )
    }

    fun query() = ChatStarTable
        .join(ChatTable, JoinType.INNER) { ChatTable.id.eq(ChatStarTable.chatId) }
        .select(columns)

    fun queryUsernames(rows: List<ResultRow>) = ChatStarTable
        .join(StarTable, JoinType.INNER) { StarTable.id.eq(ChatStarTable.starId) }
        .select(ChatStarTable.chatId, StarTable.username)
        .where { ChatStarTable.chatId.inList(rows.map { it[ChatTable.id] }) }
        .groupBy({ it[ChatStarTable.chatId] }) { Username(it[StarTable.username]) }
}

fun ResultRow.toChatPreview(usernames: List<Username>) = ChatPreview(
    chatId = ChatId(this[ChatTable.id].value),
    usernames = usernames,
    subject = this[ChatTable.subject],
    lastMessagePreview = this[ChatTable.lastMessagePreview],
    lastMessageAt = this[ChatTable.lastMessageAt],
    lastReadAt = this[ChatStarTable.lastReadAt],
    archivedAt = this[ChatStarTable.archivedAt],
    createdAt = this[ChatTable.createdAt]
)