@file:OptIn(ExperimentalSerializationApi::class)

package streetlight.server.routes

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import kabinet.console.globalConsole
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import streetlight.model.Api
import streetlight.model.data.OmniHistory
import streetlight.model.data.OmniMessage
import streetlight.model.data.OmniStatus
import streetlight.server.model.StreetlightRouting
import java.util.Collections

private val console = globalConsole.getHandle(StreetlightRouting::serveOmni.name)

fun StreetlightRouting.serveOmni() {
    val server = model
    val omni = server.service.omni

    val clients = Collections.synchronizedSet<DefaultWebSocketServerSession>(
        LinkedHashSet()
    )

    fun sendStatus() = omni.sendMessage(OmniStatus(clients.size))

    webSocket(Api.Omni.Log.path) {
        clients += this
        try {
            val history = server.dao.omni.readHistory(20)
            // console.log(history.size)

            sendMessage(OmniHistory(history))

            sendStatus()

            omni.logFlow.collect { record ->
                sendMessage(record)
            }

        } finally {
            clients -= this
            if (clients.isNotEmpty()) {
                sendStatus()
            }
        }
    }
}

suspend fun WebSocketSession.sendMessage(message: OmniMessage) {
    val bytes = defaultCbor.encodeToByteArray(OmniMessage.serializer(), message)
    send(bytes)
}

val defaultCbor = Cbor