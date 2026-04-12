@file:OptIn(ExperimentalSerializationApi::class)

package streetlight.server.routes

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.send
import kabinet.console.globalConsole
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import streetlight.model.Api
import streetlight.model.data.OmniStatus
import streetlight.server.model.StreetlightRouting
import java.util.Collections

private val console = globalConsole.getHandle(StreetlightRouting::serveOmni.name)

fun StreetlightRouting.serveOmni() {
    val omni = model.service.omni

    val clients = Collections.synchronizedSet<DefaultWebSocketServerSession>(
        LinkedHashSet()
    )

    fun sendStatus() = omni.send(OmniStatus(clients.size))

    webSocket(Api.Omni.Log.path) {
        clients += this
        try {
            sendStatus()

            omni.logFlow.collect { record ->
                val bytes = cbor.encodeToByteArray(record)
                send(bytes)
            }

        } finally {
            clients -= this
            if (clients.isNotEmpty()) {
                sendStatus()
            }
        }
    }
}

private val cbor = Cbor.Default