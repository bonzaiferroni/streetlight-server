package streetlight.server.db.services

import kampfire.api.toMarkdown
import kampfire.api.toUsername
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Message
import streetlight.model.data.MessageId
import streetlight.server.db.tables.MessageTable

object MessageQuery {
    val columns = with(MessageTable) {
        listOf(id, chainId, parentId, author, recipient, subject, content, isArchived, isRead, sentAt)
    }
}

fun messageQuery() = MessageTable.select(MessageQuery.columns)

fun ResultRow.toMessage() = Message(
    messageId = MessageId(this[MessageTable.id].value),
    chainId = MessageId(this[MessageTable.chainId].value),
    parentId = this[MessageTable.parentId]?.let { MessageId(it.value) },
    author = this[MessageTable.author].toUsername(),
    recipient = this[MessageTable.recipient].toUsername(),
    subject = this[MessageTable.subject],
    content = this[MessageTable.content].toMarkdown(),
    isArchived = this[MessageTable.isArchived],
    isRead = this[MessageTable.isRead],
    sentAt = this[MessageTable.sentAt]
)