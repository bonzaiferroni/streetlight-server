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
import kotlin.time.Clock

fun ApiScope.serveMessages() {
    val connection = provide<ConnectionService>()

    authGate {
        postApi(Api.Messages.SendNew) { request ->
            val message = request.data.takeIf { it.isValid } ?: return@postApi HttpProblem.BadRequest
            val identity = call.getIdentity()
            val callerId = identity.callerId
            val recipientId = dao.star.readIdByUsername(message.recipient) ?: return@postApi HttpProblem.NotFound
            if (dao.message.create(callerId, recipientId, message) == 0) return@postApi HttpProblem.InternalServerError
            connection.notifyMessage(recipientId, identity.username, Clock.System.now())
            Ok(Unit)
        }

        postApi(Api.Messages.SendReply) {
            val message = it.data
            val identity = call.getIdentity()
            val callerId = identity.callerId
            if (dao.message.create(callerId, message) == 0) return@postApi HttpProblem.NotAuthorized
            dao.message.readStarIds(message.chatId).forEach { recipientId ->
                connection.notifyMessage(recipientId, identity.username, Clock.System.now())
            }
            Ok(Unit)
        }

        getApi(Api.Messages.Inbox) {
            val identity = call.getIdentity()
            Ok(InboxContent(dao.message.readInbox(identity.callerId)))
        }

        getApi(Api.Messages.ReadChat, { it.toRecordId() }) {
            val chatId = it.data
            val identity = call.getIdentity()
            val messages = dao.message.readChat(identity.callerId, chatId) ?: return@getApi HttpProblem.NotAuthorized
            Ok(messages)
        }
    }
}