package streetlight.server.db.services

import kampfire.api.Username
import klutch.db.DbService
import klutch.db.model.CallerId
import org.jetbrains.exposed.v1.jdbc.insert
import streetlight.model.data.Message
import streetlight.model.data.MessageEdit
import streetlight.model.data.StarId
import streetlight.server.db.tables.MessageTable
import streetlight.server.db.tables.createMessage
import streetlight.server.utils.toStarId
import kotlin.time.Clock

class MessageTableDao: DbService() {
    suspend fun create(callerId: CallerId, recipientId: StarId, edit: MessageEdit) = dbQuery {
        MessageTable.insert {
            it.createMessage(callerId, recipientId, edit.toMessage())
        }
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
    createdAt = Clock.System.now()
)