package streetlight.server.db.services

import kampfire.api.Username
import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.db.whereWith
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.model.data.Message
import streetlight.model.data.MessageEdit
import streetlight.model.data.StarId
import streetlight.server.db.tables.MessageTable
import streetlight.server.db.tables.createMessage
import kotlin.time.Clock

class MessageTableDao: DbService() {
    suspend fun create(callerId: CallerId, recipientId: StarId, edit: MessageEdit) = dbQuery {
        MessageTable.insert {
            it.createMessage(callerId, recipientId, edit.toMessage())
        }
    }

    suspend fun readInbox(callerId: CallerId, archived: Boolean = false) = dbQuery {
        messageQuery().whereWith(MessageTable) {
            recipientId.eq(callerId) and isArchived.eq(archived)
        }.orderBy(MessageTable.sentAt).map { it.toMessage() }
    }
}

private fun MessageEdit.toMessage() = Message(
    messageId = messageId,
    chainId = chainId,
    parentId = parentId,
    author = Username.Empty, // set with trigger
    recipient = Username.Empty, // set with trigger
    subject = subject,
    content = content,
    isRead = false,
    isArchived = false,
    sentAt = Clock.System.now()
)