package streetlight.server.db.services

import kampfire.api.Username
import kampfire.utils.takeEllipsis
import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.db.whereWith
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.ChatId
import streetlight.model.data.ChatPreview
import streetlight.model.data.MessageId
import streetlight.model.data.NewMessage
import streetlight.model.data.ReplyMessage
import streetlight.model.data.StarId
import streetlight.server.db.tables.ChatStarTable
import streetlight.server.db.tables.ChatTable
import streetlight.server.db.tables.MessageTable
import streetlight.server.db.tables.StarTable
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class MessageTableDao: DbService() {
    suspend fun create(callerId: CallerId, recipientId: StarId, message: NewMessage) = dbQuery {
        val chatId = ChatId(Uuid.random())
        val messageId = MessageId(Uuid.random())
        val now = Clock.System.now()

        ChatTable.insert {
            it[ChatTable.id] = chatId.value
            it[ChatTable.subject] = message.subject
            it[ChatTable.lastMessagePreview] = message.content.value.takeEllipsis(40)
            it[ChatTable.lastMessageAt] = now
            it[ChatTable.createdAt] = now
        }

        ChatStarTable.insert {
            it[ChatStarTable.chatId] = chatId.value
            it[ChatStarTable.starId] = callerId.value
            it[ChatStarTable.lastReadAt] = now
        }

        ChatStarTable.insert {
            it[ChatStarTable.chatId] = chatId.value
            it[ChatStarTable.starId] = recipientId.value
        }

        MessageTable.insert {
            it[MessageTable.id] = messageId.value
            it[MessageTable.chatId] = chatId.value
            it[MessageTable.starId] = callerId.value
            it[MessageTable.content] = message.content.value
            it[MessageTable.sentAt] = now
        }

        ChatTable.update({ ChatTable.id.eq(chatId) }) {
            it[ChatTable.lastMessageId] = messageId.value
        }
    }

    suspend fun create(callerId: CallerId, message: ReplyMessage) = dbQuery {
        val messageId = MessageId(Uuid.random())
        val now = Clock.System.now()

        if (updateLastReadAt(callerId, message.chatId, now) != 1) return@dbQuery 0

        MessageTable.insert {
            it[MessageTable.id] = messageId.value
            it[MessageTable.chatId] = message.chatId.value
            it[MessageTable.starId] = callerId.value
            it[MessageTable.content] = message.content.value
            it[MessageTable.sentAt] = now
        }

        ChatTable.update({ ChatTable.id.eq(message.chatId) }) {
            it[ChatTable.lastMessageId] = messageId.value
            it[ChatTable.lastMessageAt] = now
            it[ChatTable.lastMessagePreview] = message.content.value.takeEllipsis(40)
        }
    }

    suspend fun readInbox(callerId: CallerId, limit: Int = 100) = dbQuery {
        val rows = ChatStarTable
            .join(ChatTable, JoinType.INNER) { ChatTable.id.eq(ChatStarTable.chatId) }
            .select(
                ChatTable.id,
                ChatTable.subject,
                ChatTable.lastMessagePreview,
                ChatTable.lastMessageAt,
                ChatTable.createdAt,
                ChatStarTable.lastReadAt,
                ChatStarTable.archivedAt
            )
            .where { ChatStarTable.starId.eq(callerId.value) }
            .orderBy(ChatTable.lastMessageAt to SortOrder.DESC)
            .limit(limit)
            .toList()

        val usernamesByChat = ChatStarTable
            .join(StarTable, JoinType.INNER) { StarTable.id.eq(ChatStarTable.starId) }
            .select(ChatStarTable.chatId, StarTable.username)
            .where {
                ChatStarTable.chatId.inList(rows.map { it[ChatTable.id] }) and
                        ChatStarTable.starId.neq(callerId.value)
            }
            .groupBy({ it[ChatStarTable.chatId] }) { Username(it[StarTable.username]) }

        rows.map { row ->
            val chatId = row[ChatTable.id]
            ChatPreview(
                chatId = ChatId(chatId.value),
                usernames = usernamesByChat[chatId].orEmpty(),
                subject = row[ChatTable.subject],
                lastMessagePreview = row[ChatTable.lastMessagePreview],
                lastMessageAt = row[ChatTable.lastMessageAt],
                lastReadAt = row[ChatStarTable.lastReadAt],
                archivedAt = row[ChatStarTable.archivedAt],
                createdAt = row[ChatTable.createdAt]
            )
        }
    }

    suspend fun readStarIds(chatId: ChatId) = dbQuery {
        ChatStarTable.select(ChatStarTable.starId).where { ChatStarTable.chatId.eq(chatId) }
            .map { StarId(it[ChatStarTable.starId].value) }
    }

    suspend fun readChat(callerId: CallerId, chatId: ChatId) = dbQuery {
        if (updateLastReadAt(callerId, chatId, Clock.System.now()) != 1) return@dbQuery null
        MessageAspect.query().whereWith(MessageTable) { this.chatId.eq(chatId) }.map { it.toMessage() }
    }

    private fun updateLastReadAt(callerId: CallerId, chatId: ChatId, now: Instant): Int {
        return ChatStarTable.update({ ChatStarTable.chatId.eq(chatId) and ChatStarTable.starId.eq(callerId) }) {
            it[ChatStarTable.lastReadAt] = now
        }
    }
}
