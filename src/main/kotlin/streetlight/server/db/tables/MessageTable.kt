package streetlight.server.db.tables

import klutch.db.SyncValueTrigger
import klutch.db.model.CallerId
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Message

object MessageTable: UuidTable("message") {
    val chainId = reference("chain_id", MessageTable, ReferenceOption.CASCADE).index()
    val parentId = reference("parent_id", MessageTable, ReferenceOption.CASCADE).index().nullable()
    val starId = reference("star_id", MessageTable, ReferenceOption.CASCADE).index()
    val author = text("author").index().default("someone")
    val subject = text("subject").nullable()
    val text = text("text")
    val isArchived = bool("is_archived").default(false)
    val isRead = bool("is_read").default(false)
    val createdAt = timestamp("created_at").index()
}

val messageAuthorSync = SyncValueTrigger(MessageTable.starId, MessageTable.author, StarTable, StarTable.username)

fun UpdateBuilder<*>.createMessage(callerId: CallerId, message: Message) {
    this[MessageTable.id] = message.messageId.value
    this[MessageTable.chainId] = message.chainId.value
    this[MessageTable.parentId] = message.parentId?.value
    this[MessageTable.starId] = callerId.value
    this[MessageTable.subject] = message.subject
    this[MessageTable.text] = message.text
    this[MessageTable.createdAt] = message.createdAt
}