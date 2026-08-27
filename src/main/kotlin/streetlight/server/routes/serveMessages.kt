package streetlight.server.routes

import kampfire.model.HttpProblem
import kampfire.model.Ok
import klutch.server.authGate
import klutch.server.postApi
import klutch.server.provide
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.ConnectionService
import streetlight.server.model.getIdentity
import kotlin.time.Clock

fun ApiScope.serveMessages() {
    val connection = provide<ConnectionService>()

    authGate {
        postApi(Api.Messages.Send) {
            val message = it.data
            val identity = call.getIdentity()
            val callerId = identity.callerId
            val recipientId = dao.star.readIdByUsername(message.recipient) ?: return@postApi HttpProblem.NotFound
            dao.message.create(callerId, recipientId, message)
            connection.notifyMessage(recipientId, identity.username, Clock.System.now())
            Ok(Unit)
        }
    }
}