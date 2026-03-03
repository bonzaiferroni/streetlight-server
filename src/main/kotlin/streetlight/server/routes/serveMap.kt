package streetlight.server.routes

import io.ktor.server.routing.Routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kabinet.console.globalConsole
import kampfire.model.meters
import kampfire.model.distanceTo
import kampfire.model.kilometers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import streetlight.model.Api
import streetlight.model.data.Spirit
import streetlight.model.data.SpiritFrame
import streetlight.model.data.SpiritId
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider
import java.util.concurrent.ConcurrentHashMap

private val console = globalConsole.getHandle(Routing::serveMap.name)

fun Routing.serveMap(app: ServerProvider = RuntimeProvider) {
    val connections = LinkedHashSet<SpiritConnection>()
    val connectionsMutex = Mutex()

    webSocket(Api.Map.SpiritVision.path) {
        var connection: SpiritConnection? = null

        try {
            for (frame in incoming) {
                val text = (frame as? Frame.Text)?.readText() ?: continue

                val msg = try {
                    text.decode()
                } catch (e: Exception) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Bad message JSON"))
                    return@webSocket
                }

                when (msg) {
                    is SpiritFrame.Initial -> {
                        if (connection != null) {
                            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Already initialized"))
                            return@webSocket
                        }

                        connection = SpiritConnection(this, msg.spirit)

                        connectionsMutex.withLock {
                            connections.add(connection)
                        }

                        console.logInfo("Spirit connected: ${msg.spirit.name} (${msg.spirit.spiritId.value})")
                    }
                    is SpiritFrame.Position -> {
                        val connection = connection ?: continue
                        // Update sender state
                        connection.spirit = connection.spirit.copy(position = msg.pos)

                        val spirit = connection.spirit
                        val kindred = connection.kindred

                        // Snapshot recipients under lock
                        val recipients = connectionsMutex.withLock {
                            val senderPoint = msg.pos
                            connections.asSequence()
                                .filter { it !== connection }
                                .filter { it.spirit.position.distanceTo(senderPoint) <= 5000.meters }
                                .toList()
                        }
                        console.log("r: ${recipients.size} of ${connections.size}")

                        val deltaText = msg.encode()
                        var initialText: String? = null

                        // Fire-and-forget relays so this receiver loop ain't blocked
                        recipients.forEach { recipient ->
                            val otherSpirit = recipient.spirit

                            if (kindred.add(otherSpirit.spiritId)) {
                                // introduce clients to each other
                                val payload = initialText ?: SpiritFrame.Initial(spirit).encode()
                                    .also { initialText = it }
                                val otherPayload = SpiritFrame.Initial(otherSpirit).encode()
                                recipient.kindred.add(spirit.spiritId)

                                launch {
                                    console.log("initial send")
                                    recipient.session.trySend(payload)
                                }
                                launch {
                                    connection.session.trySend(otherPayload)
                                }
                            } else {
                                launch {
                                    console.log("delta send")
                                    recipient.session.trySend(deltaText)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            console.logThrowable(e)
            console.logError("Error in serveMap websocket")
        } finally {
            val conn = connection
            if (conn != null) {
                connectionsMutex.withLock {
                    connections.remove(conn)
                }
                console.logInfo("Spirit disconnected: ${conn.spirit.name}")
            }
        }
    }
}

private class SpiritConnection(
    val session: DefaultWebSocketServerSession,
    var spirit: Spirit,
) {
    val kindred: MutableSet<SpiritId> = ConcurrentHashMap.newKeySet()
}

private suspend fun DefaultWebSocketServerSession.trySend(text: String) = try {
    send(text)
} catch (_: Exception) {
    // nothing yet
}

private fun SpiritFrame.encode() = Json.encodeToString<SpiritFrame>(this)
private fun String.decode() = Json.decodeFromString<SpiritFrame>(this)