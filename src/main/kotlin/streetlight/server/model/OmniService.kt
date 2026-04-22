package streetlight.server.model

import kampfire.api.StringId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import streetlight.model.data.EventId
import streetlight.model.data.Beacon
import streetlight.model.data.EventCreated
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

    private val eventLights = linkedSetOf<Pair<StarId, StringId>>()

    fun sendBeacon(starId: StarId, itemId: StringId) {
        val pair = starId to itemId
        if (!eventLights.add(pair)) return // avoids resending if user has recently lit the event
        if (eventLights.size > 1000) {     // not ideal
            eventLights.iterator().next().also { eventLights.remove(it) }
        }

        sendMessage(Beacon(itemId))
    }
}