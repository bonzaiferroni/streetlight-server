package streetlight.server.routes

import kampfire.model.HttpProblem
import kampfire.model.Ok
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi
import klutch.server.provide
import streetlight.model.Api
import streetlight.model.data.InboxContent
import streetlight.model.data.toRecordId
import streetlight.server.model.ApiScope
import streetlight.server.model.ConnectionService
import streetlight.server.model.getIdentity

fun ApiScope.serveMessages() {
    val connection = provide<ConnectionService>()

    authGate {
        postApi(Api.Messages.SendNew) { request ->
            val newMessage = request.data.takeIf { it.isValid } ?: return@postApi HttpProblem.BadRequest
            val identity = call.getIdentity()
            val recipientId = dao.star.readIdByUsername(newMessage.recipient) ?: return@postApi HttpProblem.NotFound
            val message = dao.message.create(identity, recipientId, newMessage)
            connection.notifyMessage(recipientId, message)
            Ok(Unit)
        }

        postApi(Api.Messages.SendReply) {
            val reply = it.data
            val identity = call.getIdentity()
            val message = dao.message.create(identity, reply) ?: return@postApi HttpProblem.NotAuthorized
            dao.message.readStarIds(reply.chatId).forEach { recipientId ->
                connection.notifyMessage(recipientId, message)
            }
            Ok(Unit)
        }

        getApi(Api.Messages.Inbox) {
            val identity = call.getIdentity()
            Ok(InboxContent(dao.message.readChats(identity.callerId, false)))
        }

        postApi(Api.Messages.ReadChatMessages) {
            val request = it.data
            val identity = call.getIdentity()
            val messages = dao.message.readChatMessages(identity.callerId, request.chatId, request.cursor)
                ?: return@postApi HttpProblem.NotAuthorized
            Ok(messages)
        }

        postApi(Api.Messages.ReadChats) {
            val request = it.data
            val identity = call.getIdentity()
            Ok(dao.message.readChats(identity.callerId, request.isArchive, request.cursor))
        }

        postApi(Api.Messages.ArchiveChat) {
            val chatId = it.data
            val identity = call.getIdentity()
            if (dao.message.archiveChat(identity.callerId, chatId) != 1) return@postApi HttpProblem.NotAuthorized
            Ok(Unit)
        }

        postApi(Api.Messages.UnarchiveChat) {
            val chatId = it.data
            val identity = call.getIdentity()
            if (dao.message.unarchiveChat(identity.callerId, chatId) != 1) return@postApi HttpProblem.NotAuthorized
            Ok(Unit)
        }

        getApi(Api.Messages.ReadChatPreview, { it.toRecordId() }) {
            val chatId = it.data
            val identity = call.getIdentity()
            Ok(dao.message.readChatPreview(identity.callerId, chatId) ?: return@getApi HttpProblem.NotAuthorized)
        }
    }
}