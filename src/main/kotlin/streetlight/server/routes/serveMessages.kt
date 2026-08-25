package streetlight.server.routes

import kampfire.model.HttpProblem
import kampfire.model.Ok
import klutch.server.authGate
import klutch.server.postApi
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity

fun ApiScope.serveMessages() {
    authGate {
        postApi(Api.Messages.Send) {
            val message = it.data
            val identity = call.getIdentity()
            val callerId = identity.callerId
            val recipientId = dao.star.readIdByUsername(message.recipient) ?: return@postApi HttpProblem.NotFound
            dao.message.create(callerId, recipientId, message)
            Ok(Unit)
        }
    }
}