package streetlight.server.db.services

import kampfire.utils.takeEllipsis
import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.db.model.Identity
import klutch.db.whereWith
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.ChatId
import streetlight.model.data.Message
import streetlight.model.data.MessageId
import streetlight.model.data.NewMessage
import streetlight.model.data.RecordCursor
import streetlight.model.data.ReplyMessage
import streetlight.model.data.StarId
import streetlight.server.db.tables.ChatStarTable
import streetlight.server.db.tables.ChatTable
import streetlight.server.db.tables.MessageTable
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class MessageTableDao: DbService() {
    suspend fun create(caller: Identity, recipientId: StarId, message: NewMessage) = dbQuery {
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
            it[ChatStarTable.starId] = caller.callerId.value
            it[ChatStarTable.lastReadAt] = now
        }

        ChatStarTable.insert {
            it[ChatStarTable.chatId] = chatId.value
            it[ChatStarTable.starId] = recipientId.value
        }

        MessageTable.insert {
            it[MessageTable.id] = messageId.value
            it[MessageTable.chatId] = chatId.value
            it[MessageTable.starId] = caller.callerId.value
            it[MessageTable.content] = message.content.value
            it[MessageTable.sentAt] = now
        }

        ChatTable.update({ ChatTable.id.eq(chatId) }) {
            it[ChatTable.lastMessageId] = messageId.value
        }

        Message(messageId, chatId, caller.username, message.content, now)
    }

    suspend fun create(caller: Identity, message: ReplyMessage) = dbQuery {
        val messageId = MessageId(Uuid.random())
        val now = Clock.System.now()

        if (updateLastReadAt(caller.callerId, message.chatId, now) != 1) return@dbQuery null

        MessageTable.insert {
            it[MessageTable.id] = messageId.value
            it[MessageTable.chatId] = message.chatId.value
            it[MessageTable.starId] = caller.callerId.value
            it[MessageTable.content] = message.content.value
            it[MessageTable.sentAt] = now
        }

        ChatTable.update({ ChatTable.id.eq(message.chatId) }) {
            it[ChatTable.lastMessageId] = messageId.value
            it[ChatTable.lastMessageAt] = now
            it[ChatTable.lastMessagePreview] = message.content.value.takeEllipsis(40)
        }

        Message(messageId, message.chatId, caller.username, message.content, now)
    }

    suspend fun readChats(callerId: CallerId, isArchive: Boolean, cursor: RecordCursor? = null) = dbQuery {
        val isArchiveQuery = if (isArchive) {
            ChatStarTable.archivedAt.greater(ChatTable.lastMessageAt)
        } else {
            ChatStarTable.archivedAt.isNull() or ChatStarTable.archivedAt.lessEq(ChatTable.lastMessageAt)
        }
        val rows = ChatPreviewAspect.query()
            .whereWith(ChatTable) {
                val chatMatch = ChatStarTable.starId.eq(callerId.value) and isArchiveQuery
                if (cursor == null) chatMatch
                else chatMatch and (
                    lastMessageAt.less(cursor.recordAt) or
                            (lastMessageAt.eq(cursor.recordAt) and id.less(cursor.recordId))
                )
            }
            .orderBy(ChatTable.lastMessageAt, SortOrder.DESC)
            .limit(cursor?.limit ?: RecordCursor.DefaultLimit)
            .toList()

        val chatBadges = ChatPreviewAspect.queryBadges(rows)

        rows.map { row ->
            val chatId = row[ChatTable.id]
            val badges = chatBadges[chatId].orEmpty()
            row.toChatPreview(badges)
        }
    }

    suspend fun readChatMessages(callerId: CallerId, chatId: ChatId, cursor: RecordCursor?) = dbQuery {
        if (updateLastReadAt(callerId, chatId, Clock.System.now()) != 1) return@dbQuery null
        MessageAspect.query().whereWith(MessageTable) {
            val chatMatch = this.chatId.eq(chatId)
            if (cursor == null) chatMatch
            else chatMatch and (
                    sentAt.less(cursor.recordAt) or
                            (sentAt.eq(cursor.recordAt) and id.less(cursor.recordId))
                    )
        }
            .orderBy(MessageTable.sentAt to SortOrder.DESC, MessageTable.id to SortOrder.DESC)
            .limit(cursor?.limit ?: RecordCursor.DefaultLimit)
            .map { it.toMessage() }
    }

    private fun updateLastReadAt(callerId: CallerId, chatId: ChatId, now: Instant): Int {
        return ChatStarTable.update({ ChatStarTable.chatId.eq(chatId) and ChatStarTable.starId.eq(callerId) }) {
            it[ChatStarTable.lastReadAt] = now
        }
    }

    suspend fun readStarIds(chatId: ChatId) = dbQuery {
        ChatStarTable.select(ChatStarTable.starId).where { ChatStarTable.chatId.eq(chatId) }
            .map { StarId(it[ChatStarTable.starId].value) }
    }

    suspend fun archiveChat(callerId: CallerId, chatId: ChatId) = dbQuery {
        ChatStarTable.update({ ChatStarTable.chatId.eq(chatId) and ChatStarTable.starId.eq(callerId) }) {
            it[ChatStarTable.archivedAt] = Clock.System.now()
        }
    }

    suspend fun unarchiveChat(callerId: CallerId, chatId: ChatId) = dbQuery {
        ChatStarTable.update({ ChatStarTable.chatId.eq(chatId) and ChatStarTable.starId.eq(callerId) }) {
            it[ChatStarTable.archivedAt] = null
        }
    }
}
