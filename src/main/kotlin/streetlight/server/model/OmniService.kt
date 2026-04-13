package streetlight.server.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import streetlight.model.data.EventId
import streetlight.model.data.EventPosted
import streetlight.model.data.GalaxyId
import streetlight.model.data.OmniMessage
import kotlin.time.Clock

class OmniService(private val dao: DaoFacade) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _logFlow = MutableSharedFlow<OmniMessage>(0, 8)
    val logFlow: Flow<OmniMessage> = _logFlow

    fun send(record: OmniMessage) {
        scope.launch {
            _logFlow.emit(record)
        }
    }

    suspend fun sendEventPosted(username: String, eventId: EventId, galaxyId: GalaxyId) {
        val title = dao.event.readEventTitle(eventId) ?: return
        val galaxyName = dao.galaxy.readGalaxyName(galaxyId) ?: return
        send(EventPosted(
            eventId = eventId,
            galaxyId = galaxyId,
            title = title,
            galaxy = galaxyName,
            username = username,
            recordAt = Clock.System.now()
        ))
    }
}