package streetlight.server.model

import kampfire.api.Username
import klutch.db.model.SessionId
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import streetlight.model.data.Message
import streetlight.model.data.OmniMessage
import streetlight.model.data.StarId
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** The live connections of each star, one per open socket, and the events sent to them. */
class ConnectionService() {
    private val connections = ConcurrentHashMap<StarId, Connection>()

    /** Opens the connection [connectionId] for [starId] and returns the events of that star. */
    fun openConnection(starId: StarId, connectionId: Uuid): Flow<ConnectionEvent> =
        connections.compute(starId) { _, existing ->
            (existing ?: Connection()).also { it.open(connectionId) }
        }!!.eventFlow

    /** Closes [connectionId], forgetting the star once no connection of theirs is open. */
    fun closeConnection(starId: StarId, connectionId: Uuid) {
        connections.compute(starId) { _, existing ->
            if (existing == null) null
            else if (existing.close(connectionId)) null else existing
        }
    }

    /** Tells the connections of [starId] that [sessionId] has ended. */
    fun exitSession(starId: StarId, sessionId: SessionId) {
        emit(starId) { ConnectionExit(sessionId) }
    }

    fun notifyMessage(starId: StarId, message: Message) {
        emit(starId) { ConnectionMessage(message) }
    }

    private fun emit(starId: StarId, event: () -> ConnectionEvent) {
        connections[starId]?.eventFlow?.tryEmit(event())
    }
}

private class Connection {
    val eventFlow = MutableSharedFlow<ConnectionEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val sessions = mutableSetOf<Uuid>()

    fun open(connectionId: Uuid) {
        sessions.add(connectionId)
    }

    fun close(connectionId: Uuid): Boolean {
        sessions.remove(connectionId)
        return sessions.isEmpty()
    }
}

/** An event sent to a star's connections. */
sealed interface ConnectionEvent

data class ConnectionExit(val sessionId: SessionId): ConnectionEvent
data class ConnectionMessage(val message: OmniMessage): ConnectionEvent