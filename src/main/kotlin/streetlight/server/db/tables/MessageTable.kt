package streetlight.server.db.tables

import klutch.db.SyncValueTrigger
import klutch.db.model.CallerId
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Message
import streetlight.model.data.StarId

object MessageTable: UuidTable("message") {
    val chainId = reference("chain_id", MessageTable, ReferenceOption.CASCADE).index()
    val parentId = reference("parent_id", MessageTable, ReferenceOption.CASCADE).index().nullable()
    val authorId = reference("author_id", StarTable, ReferenceOption.CASCADE).index()
    val recipientId = reference("recipient_id", StarTable, ReferenceOption.CASCADE).index().nullable()
    val author = text("author").index().default("someone")
    val recipient = text("recipient").index().default("someone")
    val subject = text("subject").nullable()
    val content = text("content")
    val isArchived = bool("is_archived").default(false)
    val isRead = bool("is_read").default(false)
    val sentAt = timestamp("sent_at").index()
}

val messageAuthorSync = SyncValueTrigger(MessageTable.authorId, MessageTable.author, StarTable, StarTable.username)
val messageRecipientSync = SyncValueTrigger(MessageTable.recipientId, MessageTable.recipient, StarTable, StarTable.username)

fun UpdateBuilder<*>.createMessage(callerId: CallerId, recipientId: StarId, message: Message) {
    this[MessageTable.id] = message.messageId.value
    this[MessageTable.chainId] = message.chainId.value
    this[MessageTable.parentId] = message.parentId?.value
    this[MessageTable.authorId] = callerId.value
    this[MessageTable.recipientId] = recipientId.value
    this[MessageTable.subject] = message.subject
    this[MessageTable.content] = message.content.value
    this[MessageTable.sentAt] = message.sentAt
}