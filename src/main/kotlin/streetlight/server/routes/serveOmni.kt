@file:OptIn(ExperimentalSerializationApi::class)

package streetlight.server.routes

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.sse.ServerSSESession
import io.ktor.server.sse.sse
import klutch.db.model.SessionIdentity
import klutch.server.authGate
import klutch.server.provide
import koala.utils.jsonConfig
import kotlinx.coroutines.flow.takeWhile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import streetlight.model.Api
import streetlight.model.data.EventCreated
import streetlight.model.data.EventUpdated
import streetlight.model.data.GalaxyFounded
import streetlight.model.data.LocationCreated
import streetlight.model.data.LocationEdited
import streetlight.model.data.Message
import streetlight.model.data.OmniHistory
import streetlight.model.data.OmniMessage
import streetlight.server.model.ApiScope
import streetlight.server.model.ConnectionExit
import streetlight.server.model.ConnectionMessage
import streetlight.server.model.ConnectionService
import streetlight.server.utils.starId
import kotlin.time.Clock
import kotlin.uuid.Uuid

// private val console = globalConsole.getHandle(ApiScope::serveOmni.name)

fun ApiScope.serveOmni() {
    val connectionService = provide<ConnectionService>()

    authGate {
        sse(Api.Omni.Log.path) {
            val principal = call.principal<SessionIdentity>() ?: error("principal not found")
            val sessionId = principal.session.sessionId
            val starId = principal.identity.starId
            val connectionId = Uuid.random()
            val eventFlow = connectionService.openConnection(starId, connectionId)

            try {
                if (call.getLastEventId() == null) {
                    sendMessage(OmniHistory(dao.omni.readHistory(20)), eventId = "connected")
                }

                eventFlow.takeWhile { it !is ConnectionExit || it.sessionId != sessionId }
                    .collect { event ->
                        when (event) {
                            is ConnectionMessage -> {
                                when (val message = event.message) {
                                    is OmniHistory -> {}
                                    else -> {
                                        sendMessage(message)
                                    }
                                }
                            }
                            is ConnectionExit -> {}
                        }
                    }

            } finally {
                connectionService.closeConnection(starId, connectionId)
            }
        }
    }
}

suspend fun ServerSSESession.sendMessage(message: OmniMessage, eventId: String? = null) {
    send(data = message.encode(), id = eventId)
}

private fun OmniMessage.encode(): String = jsonConfig.encodeToString(serializer(), this)

private fun ApplicationCall.getLastEventId() = request.headers["Last-Event-ID"]