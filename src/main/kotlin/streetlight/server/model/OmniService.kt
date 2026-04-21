package streetlight.server.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import streetlight.model.data.EventId
import streetlight.model.data.EventLighted
import streetlight.model.data.EventPosted
import streetlight.model.data.GalaxyId
import streetlight.model.data.OmniMessage
import streetlight.model.data.OmniRecord
import streetlight.model.data.StarId
import kotlin.time.Clock

class OmniService(private val dao: DaoFacade) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _logFlow = MutableSharedFlow<OmniMessage>(0, 8)
    val logFlow: Flow<OmniMessage> = _logFlow

    fun sendMessage(message: OmniMessage) {
        scope.launch {
            _logFlow.emit(message)
        }

        if (message is OmniRecord) {
            scope.launch {
                dao.omni.create(message)
            }
        }
    }

    suspend fun sendEventPosted(username: String, eventId: EventId, galaxyId: GalaxyId) {
        val title = dao.event.readEventTitle(eventId) ?: return
        val galaxyName = dao.galaxy.readGalaxyName(galaxyId) ?: return
        sendMessage(EventPosted(
            eventId = eventId,
            galaxyId = galaxyId,
            title = title,
            galaxy = galaxyName,
            username = username,
            recordAt = Clock.System.now()
        ))
    }

    private val eventLights = linkedSetOf<Pair<StarId, EventId>>()

    suspend fun sendEventLighted(starId: StarId, eventId: EventId) {
        val pair = starId to eventId
        if (!eventLights.add(pair)) return // avoids resending if user has recently lit the event
        if (eventLights.size > 1000) { // not ideal
            eventLights.iterator().next().also { eventLights.remove(it) }
        }

        val title = dao.event.readEventTitle(eventId) ?: return
        sendMessage(EventLighted(
            eventId = eventId,
            title = title,
            recordAt = Clock.System.now()
        ))
    }
}