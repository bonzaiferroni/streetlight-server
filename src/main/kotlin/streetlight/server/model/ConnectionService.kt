package streetlight.server.model

import klutch.db.model.SessionId
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import streetlight.model.data.OmniMessage
import streetlight.model.data.StarId
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

class ConnectionService() {
    private val connections = ConcurrentHashMap<StarId, Connection>()

    fun openConnection(starId: StarId, connectionId: Uuid): Flow<ConnectionEvent> =
        connections.compute(starId) { _, existing ->
            (existing ?: Connection()).also { it.open(connectionId) }
        }!!.eventFlow

    fun closeConnection(starId: StarId, connectionId: Uuid) {
        connections.compute(starId) { _, existing ->
            if (existing == null) null
            else if (existing.close(connectionId)) null else existing
        }
    }

    fun exitSession(starId: StarId, sessionId: SessionId) {
        connections[starId]?.exit(sessionId)
    }
}

private class Connection {
    private val _eventFlow = MutableSharedFlow<ConnectionEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val eventFlow: Flow<ConnectionEvent> = _eventFlow
    private val sessions = mutableSetOf<Uuid>()

    fun open(connectionId: Uuid) {
        sessions.add(connectionId)
    }

    fun close(connectionId: Uuid): Boolean {
        sessions.remove(connectionId)
        return sessions.isEmpty()
    }

    fun exit(sessionId: SessionId) {
        _eventFlow.tryEmit(ConnectionExit(sessionId))
    }
}

sealed interface ConnectionEvent

data class ConnectionExit(val sessionId: SessionId): ConnectionEvent
data class ConnectionMessage(val message: OmniMessage): ConnectionEvent