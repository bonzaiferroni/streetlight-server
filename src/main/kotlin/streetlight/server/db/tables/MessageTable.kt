package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

object MessageTable: UuidTable("message") {
    val chatId = reference("chat_id", ChatTable, ReferenceOption.CASCADE)
    val starId = reference("star_id", StarTable, ReferenceOption.SET_NULL).index().nullable()
    val content = text("content")
    val sentAt = timestamp("sent_at").index()

    init {
        index("message_chat_sent", false, chatId, sentAt)
    }
}

object ChatTable: UuidTable("chat") {
    val lastMessageId = reference("last_message_id", MessageTable, ReferenceOption.SET_NULL).nullable()
    val subject = text("subject").nullable()
    val lastMessagePreview = text("last_message_preview")
    val lastMessageAt = timestamp("last_message_at")
    val createdAt = timestamp("created_at")
}

/** The members of each chat, with when each last read it and archived it. */
object ChatStarTable: Table("chat_star") {
    val chatId = reference("chat_id", ChatTable, ReferenceOption.CASCADE)
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE).index()
    val lastReadAt = timestamp("last_read_at").nullable()
    val archivedAt = timestamp("archived_at").nullable()

    override val primaryKey = PrimaryKey(starId, chatId)
}

//fun UpdateBuilder<*>.createMessage(callerId: CallerId, recipientId: StarId, message: Message) {
//    this[MessageTable.id] = message.messageId.value
//    this[MessageTable.chatId] = message.chatId.value
//    this[MessageTable.parentId] = message.parentId?.value
//    this[MessageTable.starId] = callerId.value
//    this[MessageTable.recipientId] = recipientId.value
//    this[MessageTable.subject] = message.subject
//    this[MessageTable.content] = message.content.value
//    this[MessageTable.sentAt] = message.sentAt
//}