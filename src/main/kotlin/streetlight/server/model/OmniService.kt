package streetlight.server.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import streetlight.model.data.OmniMessage

class OmniService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _logFlow = MutableSharedFlow<OmniMessage>(0, 8)
    val logFlow: Flow<OmniMessage> = _logFlow

    fun send(record: OmniMessage) {
        scope.launch {
            _logFlow.emit(record)
        }
    }
}