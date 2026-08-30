package streetlight.server.db.services

import kampfire.api.Username
import kampfire.api.toMarkdown
import kampfire.api.toUsername
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.ChatId
import streetlight.model.data.Message
import streetlight.model.data.MessageId
import streetlight.server.db.tables.ChatStarTable
import streetlight.server.db.tables.ChatTable
import streetlight.server.db.tables.MessageTable
import streetlight.server.db.tables.StarTable

object MessageAspect {
    val columns = with(MessageTable) {
        listOf(id, chatId, content, sentAt, StarTable.username)
    }

    fun query() = MessageTable.leftJoin(StarTable).select(columns)
}


fun ResultRow.toMessage() = Message(
    messageId = MessageId(this[MessageTable.id].value),
    chatId = ChatId(this[MessageTable.chatId].value),
    author = this[StarTable.username].toUsername(),
    content = this[MessageTable.content].toMarkdown(),
    sentAt = this[MessageTable.sentAt]
)