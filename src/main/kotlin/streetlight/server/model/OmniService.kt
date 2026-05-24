package streetlight.server.model

import kampfire.api.Slug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import streetlight.model.data.EventCreated
import streetlight.model.data.EventUpdated
import streetlight.model.data.GalaxyFounded
import streetlight.model.data.OmniMessage
import streetlight.model.data.OmniRecord
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

    fun sendGalaxyFounded(name: String, slug: Slug, username: String) = sendMessage(
        GalaxyFounded(slug = slug, name = name, username = username, recordAt = Clock.System.now())
    )

    fun sendEventCreated(title: String, slug: Slug, username: String) = sendMessage(
        EventCreated(slug = slug, title = title, username = username, recordAt = Clock.System.now())
    )

    fun sendEventUpdated(title: String, slug: Slug, username: String) = sendMessage(
        EventUpdated(slug = slug, title = title, username = username, recordAt = Clock.System.now())
    )
}
