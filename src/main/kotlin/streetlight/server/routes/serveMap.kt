package streetlight.server.routes

import io.ktor.server.routing.Routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kabinet.console.globalConsole
import kampfire.model.meters
import kampfire.model.distanceTo
import kotlinx.serialization.json.Json
import streetlight.model.Api
import streetlight.model.data.Spirit
import streetlight.model.data.SpiritDelta
import streetlight.model.data.SpiritPointDelta
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider
import java.util.Collections

private val console = globalConsole.getHandle(Routing::serveMap.name)

fun Routing.serveMap(app: ServerProvider = RuntimeProvider) {
    val connections = Collections.synchronizedSet<SpiritConnection>(LinkedHashSet())

    webSocket(Api.Map.SpiritVision.path) {
        var myConnection: SpiritConnection? = null
        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    if (myConnection == null) {
                        val initialSpirit = Json.decodeFromString<Spirit>(text)
                        myConnection = SpiritConnection(this, initialSpirit)
                        connections.add(myConnection)
                        console.logInfo("Spirit connected: ${initialSpirit.label} (${initialSpirit.spiritId})")
                    } else {
                        val delta = Json.decodeFromString<SpiritDelta>(text)
                        when (delta) {
                            is SpiritPointDelta -> {
                                // Update local state for the sender
                                myConnection.spirit = myConnection.spirit.copy(geoPoint = delta.geoPoint)
                                
                                // Relay to others within 500m
                                val senderPoint = delta.geoPoint
                                val relayText = Json.encodeToString<SpiritDelta>(delta)
                                
                                val recipients = synchronized(connections) {
                                    connections.filter { it != myConnection && it.spirit.geoPoint.distanceTo(senderPoint) <= 500.meters }.toList()
                                }
                                
                                recipients.forEach { recipient ->
                                    recipient.session.send(relayText)
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
            myConnection?.let { 
                connections.remove(it)
                console.logInfo("Spirit disconnected: ${it.spirit.label}")
            }
        }
    }
}

private data class SpiritConnection(
    val session: DefaultWebSocketServerSession,
    var spirit: Spirit
)
