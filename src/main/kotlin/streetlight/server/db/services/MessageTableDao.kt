package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.model.CallerId
import org.jetbrains.exposed.v1.jdbc.insert
import streetlight.model.data.Message
import streetlight.model.data.MessageId
import streetlight.model.data.StarId
import streetlight.server.db.tables.MessageTable
import streetlight.server.db.tables.createMessage
import kotlin.uuid.Uuid

class MessageTableDao: DbService() {
    suspend fun create(callerId: CallerId, message: Message) = dbQuery {
        MessageTable.insert {
            it.createMessage(callerId, message.copy(messageId = MessageId(Uuid.random())))
        }
    }

    
}