package streetlight.server.plugins

import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import streetlight.server.model.ServerScope
import streetlight.server.routes.initGtfs

fun Application.configureTransit(server: ServerScope) {
    launch(Dispatchers.IO) {
        with(server) {
            initGtfs()
        }
    }
}
