package streetlight.server.db.services

import kampfire.api.toUsername
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.ChatId
import streetlight.model.data.ChatPreview
import streetlight.model.data.StarBadge
import streetlight.server.db.tables.ChatStarTable
import streetlight.server.db.tables.ChatTable
import streetlight.server.db.tables.StarTable

/** Reads chats as a star's previews of them. */
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

    /** The badges of the members of each chat in [rows]. */
    fun queryBadges(rows: List<ResultRow>) = ChatStarTable
        .join(StarTable, JoinType.INNER) { StarTable.id.eq(ChatStarTable.starId) }
        .select(ChatStarTable.chatId, StarTable.username, StarTable.image)
        .where { ChatStarTable.chatId.inList(rows.map { it[ChatTable.id] }) }
        .groupBy({ it[ChatStarTable.chatId] }) { it.toStarBadge() }
}

fun ResultRow.toChatPreview(badges: List<StarBadge>) = ChatPreview(
    chatId = ChatId(this[ChatTable.id].value),
    badges = badges,
    subject = this[ChatTable.subject],
    lastMessagePreview = this[ChatTable.lastMessagePreview],
    lastMessageAt = this[ChatTable.lastMessageAt],
    lastReadAt = this[ChatStarTable.lastReadAt],
    archivedAt = this[ChatStarTable.archivedAt],
    createdAt = this[ChatTable.createdAt]
)

fun ResultRow.toStarBadge() = StarBadge(
    username = this[StarTable.username].toUsername(),
    thumb = this[StarTable.image]?.thumb
)